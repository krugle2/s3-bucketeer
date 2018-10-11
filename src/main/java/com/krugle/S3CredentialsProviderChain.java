package com.krugle;

import java.io.Serializable;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;

@SuppressWarnings("serial")
public class S3CredentialsProviderChain implements AWSCredentialsProvider, Serializable {

    private String _accessKey;
    private String _secretKey;

    public S3CredentialsProviderChain(String accessKey, String secretKey) {
        _accessKey = accessKey;
        _secretKey = secretKey;
    }

    @Override
    public AWSCredentials getCredentials() {
        return new AWSCredentials() {

            @Override
            public String getAWSAccessKeyId() {
                return _accessKey;
            }

            @Override
            public String getAWSSecretKey() {
                return _secretKey;
            }
        };
    }

    @Override
    public void refresh() {
    }
}
