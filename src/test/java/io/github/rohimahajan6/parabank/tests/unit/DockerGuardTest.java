package io.github.rohimahajan6.parabank.tests.unit;

import io.github.rohimahajan6.parabank.api.ApiClient;
import io.github.rohimahajan6.parabank.config.TestEnv;
import io.github.rohimahajan6.parabank.guard.DockerGuard;
import io.github.rohimahajan6.parabank.guard.WriteBlockedException;
import java.math.BigDecimal;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** No network: these checks prove the guard stops writes before any request is sent. */
public class DockerGuardTest {

    private static final String PUBLIC_API = "https://parabank.parasoft.com/parabank/services/bank";
    private static final String DOCKER_API = "http://localhost:8080/parabank/services/bank";

    @Test(groups = "unit", description = "TEST_ENV=public blocks writes")
    public void publicEnvBlocksWrites() {
        assertThatThrownBy(() -> DockerGuard.requireWritable("transfer", TestEnv.PUBLIC, DOCKER_API))
                .isInstanceOf(WriteBlockedException.class)
                .hasMessageContaining("TEST_ENV=PUBLIC");
    }

    @Test(groups = "unit", description = "The public host is blocked even with TEST_ENV=docker")
    public void publicHostBlockedEvenInDockerMode() {
        assertThatThrownBy(() -> DockerGuard.requireWritable("transfer", TestEnv.DOCKER, PUBLIC_API))
                .isInstanceOf(WriteBlockedException.class)
                .hasMessageContaining("public ParaBank site");
    }

    @Test(groups = "unit", description = "A missing or malformed target URL is treated as unsafe")
    public void unknownTargetIsBlocked() {
        assertThatThrownBy(() -> DockerGuard.requireWritable("transfer", TestEnv.DOCKER, null))
                .isInstanceOf(WriteBlockedException.class);
        assertThatThrownBy(() -> DockerGuard.requireWritable("transfer", TestEnv.DOCKER, "not a url"))
                .isInstanceOf(WriteBlockedException.class);
    }

    @Test(groups = "unit", description = "Docker env with a local target is allowed")
    public void dockerEnvWithLocalTargetIsAllowed() {
        assertThatCode(() -> DockerGuard.requireWritable("transfer", TestEnv.DOCKER, DOCKER_API))
                .doesNotThrowAnyException();
    }

    @Test(groups = "unit", description = "ApiClient.transfer is blocked in public mode before any HTTP call")
    public void apiClientWriteIsBlockedInPublicMode() {
        ApiClient publicClient = new ApiClient(PUBLIC_API, TestEnv.PUBLIC);

        assertThatThrownBy(() -> publicClient.transfer(1L, 2L, new BigDecimal("1.00")))
                .isInstanceOf(WriteBlockedException.class);
    }
}
