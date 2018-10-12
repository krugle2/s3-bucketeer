/*
 *  Copyright 2018 Aragon Consulting Group
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

        final String bucketName = options.getBucket();
        final String region = options.getRegion();
        
        try {
            AWSCredentialsProvider credentials = new S3CredentialsProviderChain(options.getAccessKey(), options.getSecretKey());
            AmazonS3 s3Client = AmazonS3ClientBuilder
                            .standard()
                            .withCredentials(credentials)
                            .withRegion(region)
                            .build();

            Iterator<String> iter = new S3DirectoryLister(credentials, region, bucketName, options.getPath(), false);
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

    private static void printUsageAndExit(CmdLineParser parser) {
        parser.printUsage(System.err);
        System.exit(-1);
    }

    private static class FixS3BucketPermissionsOptions {

        private String _bucket;
        private String _path;
        private String _region = Regions.US_WEST_2.getName();
        private String _accessKey;
        private String _secretKey;

        @Option(name = "-bucket", usage = "S3 bucket", required = true)
        public void setBucket(String bucket) {
            _bucket = bucket;
        }

        public String getBucket() {
            return _bucket;
        }

        @Option(name = "-path", usage = "path (prefix) for files in bucket", required = true)
        public void setPath(String path) {
            _path = path;
        }

        public String getPath() {
            return _path;
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
