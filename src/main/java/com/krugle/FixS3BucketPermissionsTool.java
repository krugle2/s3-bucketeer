package com.krugle;

import java.util.Iterator;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;

public class FixS3BucketPermissionsTool {
    private static final Logger LOGGER = LoggerFactory.getLogger(FixS3BucketPermissionsTool.class);
    
    public static void main(String[] args) {
        FixS3BucketPermissionsOptions options = new FixS3BucketPermissionsOptions();
        CmdLineParser parser = new CmdLineParser(options);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            printUsageAndExit(parser);
        }

        final String bucketName = options.getBucket(); // "backups.aragoncg.com";
        final String region = options.getRegion(); // Regions.US_EAST_2.getName();
        
        // "AKIAINCKT5CHMLO6ZNCA"
        // "OuataHbmywHD5ZmaX6NPmRwtllUczGdzGvFRlbkK"
        
        try {
            AWSCredentialsProvider credentials = new S3CredentialsProviderChain(options.getAccessKey(), options.getSecretKey());
            AmazonS3 s3Client = AmazonS3ClientBuilder
                            .standard()
                            .withCredentials(credentials)
                            .withRegion(region)
                            .build();

            Iterator<String> iter = new S3DirectoryLister(credentials, region, bucketName, "public_html/", false);
            int fileCount = 0;
            while (iter.hasNext()) {
                String path = iter.next();
                
                // FUTURE - use a thread pool, for multi-threading of requests.
                s3Client.setObjectAcl(bucketName, path, CannedAccessControlList.BucketOwnerFullControl);
                
                fileCount++;
                LOGGER.info("{}: {}", fileCount, path);
            }
        } catch (Throwable t) {
            System.err.println("Error running tool: " + t.getMessage());
            System.exit(-1);
        }
    }

    private static class FixS3BucketPermissionsOptions {

        private String _bucket;
        private String _region = Regions.US_EAST_2.getName();
        private String _accessKey;
        private String _secretKey;

        @Option(name = "-bucket", usage = "S3 bucket", required = true)
        public void setBucket(String bucket) {
            _bucket = bucket;
        }

        public String getBucket() {
            return _bucket;
        }

        @Option(name = "-region", usage = "AWS region (default = us-east-2)", required = false)
        public void setRegion(String region) {
            _region = region;
        }

        public String getRegion() {
            return _region;
        }
        
        // FUTURE prompt user for access key if it's not been provided.
        @Option(name = "-accesskey", usage = "AWS access key", required = true)
        public void setAccessKey(String accessKey) {
            _accessKey = accessKey;
        }

        public String getAccessKey() {
            return _accessKey;
        }
        
        // FUTURE prompt user for access key if it's not been provided.
        // Use password masking, https://stackoverflow.com/questions/10819469/hide-input-on-command-line
        @Option(name = "-secretkey", usage = "AWS secret key", required = true)
        public void setSecretKey(String secretKey) {
            _secretKey = secretKey;
        }

        public String getSecretKey() {
            return _secretKey;
        }
        
        
    }

}
