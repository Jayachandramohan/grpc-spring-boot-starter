package net.devh.test.grpc;

import static io.grpc.Status.Code.UNIMPLEMENTED;
import static net.devh.test.grpc.util.GrpcAssertions.assertFutureThrowsStatus;
import static net.devh.test.grpc.util.GrpcAssertions.assertThrowsStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import com.google.protobuf.Empty;

import io.grpc.Channel;
import io.grpc.internal.testing.StreamRecorder;
import lombok.extern.slf4j.Slf4j;
import net.devh.springboot.autoconfigure.grpc.client.GrpcClient;
import net.devh.test.grpc.proto.Counter;
import net.devh.test.grpc.proto.TestServiceGrpc;
import net.devh.test.grpc.proto.TestServiceGrpc.TestServiceBlockingStub;
import net.devh.test.grpc.proto.TestServiceGrpc.TestServiceFutureStub;
import net.devh.test.grpc.proto.TestServiceGrpc.TestServiceStub;
import net.devh.test.grpc.proto.Version;

/**
 * A test checking that the server and client can start and connect to each other with minimal
 * config.
 *
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 */
@Slf4j
public abstract class AbstractSimpleServerClientTest {

    @GrpcClient("test")
    protected Channel channel;
    @GrpcClient("test")
    protected TestServiceStub testServiceStub;
    @GrpcClient("test")
    protected TestServiceBlockingStub testServiceBlockingStub;
    @GrpcClient("test")
    protected TestServiceFutureStub testServiceFutureStub;

    /**
     * Test successful call.
     *
     * @throws ExecutionException Should never happen.
     * @throws InterruptedException Should never happen.
     */
    @Test
    @DirtiesContext
    public void testSuccessfulCall() throws InterruptedException, ExecutionException {
        log.info("--- Starting tests with successful call ---");
        assertEquals("1.2.3",
                TestServiceGrpc.newBlockingStub(this.channel).getVersion(Empty.getDefaultInstance()).getVersion());

        final StreamRecorder<Version> streamRecorder = StreamRecorder.create();
        this.testServiceStub.getVersion(Empty.getDefaultInstance(), streamRecorder);
        assertEquals("1.2.3", streamRecorder.firstValue().get().getVersion());
        assertEquals("1.2.3", this.testServiceBlockingStub.getVersion(Empty.getDefaultInstance()).getVersion());
        assertEquals("1.2.3", this.testServiceFutureStub.getVersion(Empty.getDefaultInstance()).get().getVersion());
        log.info("--- Test completed ---");
    }

    /**
     * Test failing call.
     */
    @Test
    @DirtiesContext
    public void testFailingCall() {
        log.info("--- Starting tests with failing call ---");
        assertThrowsStatus(UNIMPLEMENTED,
                () -> TestServiceGrpc.newBlockingStub(this.channel).increment(Empty.getDefaultInstance()));

        final StreamRecorder<Counter> streamRecorder = StreamRecorder.create();
        this.testServiceStub.increment(Empty.getDefaultInstance(), streamRecorder);
        assertFutureThrowsStatus(UNIMPLEMENTED, streamRecorder.firstValue());
        assertThrowsStatus(UNIMPLEMENTED,
                () -> this.testServiceBlockingStub.increment(Empty.getDefaultInstance()));
        assertFutureThrowsStatus(UNIMPLEMENTED, this.testServiceFutureStub.increment(Empty.getDefaultInstance()));
        log.info("--- Test completed ---");
    }

}
