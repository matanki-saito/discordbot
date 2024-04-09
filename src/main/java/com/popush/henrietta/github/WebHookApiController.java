package com.popush.henrietta.github;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebHookApiController {
    private final WebHookAsyncService webHookAsyncService;

    @Value("${webhook.secret}")
    private String secret;

    private HmacUtils mac;

    @PostConstruct
    void setup() {
        mac = new HmacUtils(HmacAlgorithms.HMAC_SHA_1, secret);
    }

    @ResponseBody
    @PostMapping("/webhook")
    public String release(@RequestHeader(value = "X-GitHub-Event") String xGitHubEvent,
                          @RequestHeader(value = "X-GitHub-Delivery") String xGitHubDelivery,
                          @RequestHeader(value = "X-Hub-Signature") String xHubSignature,
                          @RequestBody String payload
    ) throws OtherSystemException, InterruptedException, ArgumentException {
        if (!checkHmac(xHubSignature, payload)) {
            log.info("invalid");
            return "hmac is invalid";
        }

        var webhookResponse = switch (xGitHubEvent) {
            case "issue_comment" ->
                    findWebhookResponse(payload, GitHubIssueCommentWebhookResponse.class, List.of("created", "deleted", "edited"));
            case "issues" ->
                    findWebhookResponse(payload, GithubIssuesWebhookResponse.class, List.of("opened", "closed"));
            default -> Optional.<GithubWebhookResponse>empty();
        };

        if (webhookResponse.isEmpty()) {
            return "skip";
        }

        var process = webHookAsyncService.makeProcess(webhookResponse.get());

        process.thenAcceptAsync(heavyProcessResult -> log.info("finished"))
                .exceptionally(e -> {
                    log.warn(e.getMessage());
                    return null;
                });

        return "success";
    }

    private boolean checkHmac(@NonNull String xHubSignature, @NonNull String payload) {
        var keyValue = xHubSignature.split("=");

        return keyValue.length == 2
                && keyValue[0].equalsIgnoreCase("sha1")
                && keyValue[1].length() == 40
                && mac.hmacHex(payload).equalsIgnoreCase(keyValue[1]);
    }

    private <T extends GithubWebhookResponse> Optional<T> findWebhookResponse(
            String payload,
            Class<T> expectClass,
            List<String> expectActions
    ) throws OtherSystemException, ArgumentException {
        T result;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            result = mapper.readValue(payload, expectClass);
        } catch (JsonParseException | JsonMappingException e) {
            throw new GitHubResourceException("Unmatched format. Check github API release note.", e);
        } catch (IOException e) {
            throw new MachineException("Object mapper IO Exception", e);
        }

        if (result == null) {
            throw new ArgumentException("data is null");
        }

        if (!expectActions.contains(result.getAction())) {
            return Optional.empty();
        }

        return Optional.of(result);
    }
}
