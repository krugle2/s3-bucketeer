package com.krugle;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@SuppressWarnings("serial")
public class S3DirectoryLister implements Iterator<String>, Serializable {

    private static final int OBJECTS_PER_REQUEST = 1000;
    
    private AmazonS3 _s3Client;
    private Iterator<String> _filesOrDirs;
    
    public S3DirectoryLister(String region, String bucketName, String path) {
        this(region, bucketName, path, false);
    }

    public S3DirectoryLister(String region, String bucketName, String path, boolean onlyDirs) {
        _s3Client = AmazonS3ClientBuilder
                .standard()
                .withRegion(region)
                .build();

        _filesOrDirs = makeIterator(bucketName, path, onlyDirs);
    }

    public S3DirectoryLister(AWSCredentialsProvider credentials, String region, String bucketName, String path) {
        this(credentials, region, bucketName, path, false);
    }

    public S3DirectoryLister(AWSCredentialsProvider credentials, String region, String bucketName, String path, boolean onlyDirs) {
        _s3Client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(credentials)
                .withRegion(region)
                .build();

        _filesOrDirs = makeIterator(bucketName, path, onlyDirs);
    }

    private Iterator<String> makeIterator(String bucketName, String path, boolean onlyDirs) {
        if (onlyDirs) {
            return makeDirsIterator(bucketName, path);
        } else {
            return makeFilesIterator(bucketName, path);
        }
    }
    
    private Iterator<String> makeFilesIterator(String bucketName, String path) {

        final ListObjectsRequest listRequest = new ListObjectsRequest()
                .withBucketName(bucketName)
                .withMaxKeys(OBJECTS_PER_REQUEST);

        if (path != null) {
            listRequest.withPrefix(path);
        }

        return new Iterator<String>() {

            private boolean _finished = false;
            private ObjectListing _listObjects = _s3Client.listObjects(listRequest);
            private Iterator<S3ObjectSummary> _listIter = null;
            private String _next = null;

            @Override
            public boolean hasNext() {
                while (!_finished && (_next == null)) {
                    if (_listIter == null) {
                        _listIter = _listObjects.getObjectSummaries().iterator();
                    }

                    if (!_listIter.hasNext()) {
                        _listIter = null;

                        if (_listObjects.isTruncated()) {
                            _listObjects = _s3Client.listNextBatchOfObjects(_listObjects);
                        } else {
                            _finished = true;
                        }
                    } else {
                        String key = _listIter.next().getKey();

                        if (!key.endsWith("/")) {
                            // It's a file, not a directory.
                            _next = key;
                        }
                    }
                }

                return _next != null;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                String result = _next;
                _next = null;
                return result;
            }
        };

    }

    private Iterator<String> makeDirsIterator(String bucketName, String path) {

        final ListObjectsRequest listRequest = new ListObjectsRequest()
                .withBucketName(bucketName)
                .withMaxKeys(OBJECTS_PER_REQUEST)
                .withDelimiter("/");

        if (path != null) {
            listRequest.withPrefix(path);
        }

        return new Iterator<String>() {

            private boolean _finished = false;
            private ObjectListing _listObjects = _s3Client.listObjects(listRequest);
            private Iterator<String> _listIter = null;
            private String _next = null;

            @Override
            public boolean hasNext() {
                while (!_finished && (_next == null)) {
                    if (_listIter == null) {
                        _listIter = _listObjects.getCommonPrefixes().iterator();
                    }

                    if (!_listIter.hasNext()) {
                        _listIter = null;

                        if (_listObjects.isTruncated()) {
                            _listObjects = _s3Client.listNextBatchOfObjects(_listObjects);
                        } else {
                            _finished = true;
                        }
                    } else {
                        _next = _listIter.next();
                    }
                }

                return _next != null;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                String result = _next;
                _next = null;
                return result;
            }
        };

    }

    @Override
    public boolean hasNext() {
        return _filesOrDirs.hasNext();
    }

    @Override
    public String next() {
        return _filesOrDirs.next();
    }

}
