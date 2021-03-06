package com.box.boxjavalibv2.filetransfer;

import com.box.boxjavalibv2.dao.BoxCollection;
import com.box.boxjavalibv2.dao.BoxFile;
import com.box.boxjavalibv2.dao.BoxResourceType;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxMalformedResponseException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.boxjavalibv2.httpentities.MultipartEntityWithProgressListener.InterruptedMultipartException;
import com.box.boxjavalibv2.interfaces.IFileTransferListener;
import com.box.boxjavalibv2.requests.UploadFileRequest;
import com.box.boxjavalibv2.requests.UploadNewVersionFileRequest;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileUploadRequestObject;
import com.box.boxjavalibv2.resourcemanagers.BoxFilesManager;
import com.box.restclientv2.exceptions.BoxRestException;
import com.box.restclientv2.interfaces.IBoxConfig;

/**
 * Contains logic for uploading a user's file via Box API and supports using {@link IFileTransferListener} to monitor uploading progress.
 */
public class BoxFileUpload {

    /** Config. */
    private final IBoxConfig mConfig;

    /**
     * Constructor.
     * 
     * @param config
     *            config
     */
    public BoxFileUpload(final IBoxConfig config) {
        this.mConfig = config;
    }

    /**
     * Execute the upload task.
     * 
     * @param manager
     *            BoxFilesManager
     * @param requestObject
     *            request objecct
     * @return the uploaded BoxFile
     * @throws BoxServerException
     *             exception
     * @throws BoxRestException
     *             exception
     * @throws AuthFatalFailureException
     *             exception indicating authentication totally failed
     * @throws InterruptedException
     *             interrupted exception.
     */
    public BoxFile execute(BoxFilesManager manager, BoxFileUploadRequestObject requestObject) throws BoxServerException, BoxRestException,
        AuthFatalFailureException, InterruptedException {
        UploadFileRequest request = new UploadFileRequest(mConfig, manager.getObjectMapper(), requestObject);
        try {
            Object result = manager.getResponseAndParse(request, BoxResourceType.FILES, manager.getObjectMapper());
            BoxCollection collection = (BoxCollection) manager.tryCastObject(BoxResourceType.FILES, result);
            return BoxFilesManager.getFiles(collection).get(0);
        }
        catch (BoxRestException e) {
            if (isInterruptedMultipartException(e)) {
                throw new InterruptedException();
            }
            else {
                throw e;
            }
        }
    }

    /**
     * Upload a new version of file with known file id and sha1.
     * 
     * @param fileId
     *            id of the file
     * @param requestObject
     *            request objecct
     * @return the new FileVersionV2 object
     * @throws BoxServerException
     *             exception
     * @throws BoxRestException
     *             exception
     * @throws AuthFatalFailureException
     *             exception
     * @throws InterruptedException
     *             interrupted exception.
     */
    public BoxFile execute(final String fileId, BoxFilesManager manager, BoxFileUploadRequestObject requestObject) throws BoxServerException, BoxRestException,
        AuthFatalFailureException, InterruptedException {
        UploadNewVersionFileRequest request = new UploadNewVersionFileRequest(mConfig, manager.getObjectMapper(), fileId, requestObject);
        try {
            Object result = manager.getResponseAndParse(request, BoxResourceType.FILE_VERSIONS, manager.getObjectMapper());
            BoxCollection versions = (BoxCollection) manager.tryCastObject(BoxResourceType.FILE_VERSIONS, result);
            if (versions.getTotalCount() != 1) {
                throw new BoxMalformedResponseException(request.getExpectedResponseCode());
            }
            return (BoxFile) versions.getEntries().get(0);
        }
        catch (BoxRestException e) {
            if (isInterruptedMultipartException(e)) {
                throw new InterruptedException();
            }
            else {
                throw e;
            }
        }
    }

    private boolean isInterruptedMultipartException(BoxRestException e) {
        Throwable t = e.getCause();
        return t != null && t instanceof InterruptedMultipartException;
    }
}
