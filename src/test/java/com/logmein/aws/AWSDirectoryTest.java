package com.logmein.aws;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

public class AWSDirectoryTest {

    private static AWSDirectory awsDir;

    private static final String AWS_FOLDER_PATH = "src/test/resources/aws";

    private static final File expectedAwsDir = new File(System.getProperty("user.dir"),
            AWS_FOLDER_PATH);

    private static final File expectedDeviceFarmDir = new File(expectedAwsDir, "devicefarm");

    @BeforeClass
    public static void setup() {
        awsDir = new AWSDirectory(new File(AWS_FOLDER_PATH));
    }

    /**
     * Test {@link AWSDirectory#getLocation()}.
     */
    @Test
    public void getLocation() {
        assertThat(awsDir.getLocation().getAbsolutePath(), is(expectedAwsDir.getAbsolutePath()));
    }

    /**
     * Test {@link AWSDirectory#getPrivateKey()}.
     */
    @Test
    public void getPrivateKey() {
        assertThat(awsDir.getPrivateKey().getAbsolutePath(), is(new File(expectedDeviceFarmDir,
                AWSDirectory.PRI_KEY_PEM).getAbsolutePath()));
    }

    /**
     * Test {@link AWSDirectory#getPublicKey()}.
     */
    @Test
    public void getPublicKey() {
        assertThat(awsDir.getPublicKey().getAbsolutePath(), is(new File(expectedDeviceFarmDir,
                AWSDirectory.PUB_KEY_PUB).getAbsolutePath()));
    }

    /**
     * Test {@link AWSDirectory#getPublicKeyContents()}.
     */
    @Test
    public void getPublicKeyContents() {
        assertThat(awsDir.getPublicKeyContents(), is("this is a dummy key"));
    }

}
