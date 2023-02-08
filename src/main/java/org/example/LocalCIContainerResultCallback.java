package org.example;

import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.WaitResponse;

public class LocalCIContainerResultCallback extends WaitContainerResultCallback {
    @Override
    public void onNext(WaitResponse item) {
        super.onNext(item);
        int exitCode = item.getStatusCode();
        if (exitCode != 0) {
            throw new IllegalStateException("The script exited with a non-zero status code: " + exitCode);
        }
    }
}
