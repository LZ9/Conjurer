package com.lodz.android.conjurer.ocr;


import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.lodz.android.conjurer.bean.OcrResultBean;
import com.lodz.android.conjurer.camera.CameraManager;


/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
final class CaptureActivityHandler extends Handler {

    private static final String TAG = CaptureActivityHandler.class.getSimpleName();

    private final CaptureActivity activity;
    private final DecodeThread decodeThread;
    private static State state;
    private final CameraManager cameraManager;

    private enum State {
        PREVIEW,
        PREVIEW_PAUSED,
        CONTINUOUS,
        CONTINUOUS_PAUSED,
        SUCCESS,
        DONE
    }

    CaptureActivityHandler(CaptureActivity activity, CameraManager cameraManager, boolean isContinuousModeActive) {
        this.activity = activity;
        this.cameraManager = cameraManager;

        // Start ourselves capturing previews (and decoding if using continuous recognition mode).
        cameraManager.startPreview();

        decodeThread = new DecodeThread(activity);
        decodeThread.start();

        if (isContinuousModeActive) {
            state = State.CONTINUOUS;

            // Show the shutter and torch buttons
            activity.setButtonVisibility(true);

            // Display a "be patient" message while first recognition request is running
            activity.setStatusViewForContinuous();

            restartOcrPreviewAndDecode();
        } else {
            state = State.SUCCESS;

            // Show the shutter and torch buttons
            activity.setButtonVisibility(true);

            restartOcrPreview();
        }
    }

    @Override
    public void handleMessage(Message message) {

        switch (message.what) {
            case Constant.CJ_RESTART_PREVIEW:
                restartOcrPreview();
                break;
            case Constant.CJ_OCR_CONTINUOUS_DECODE_FAILED:
                DecodeHandler.resetDecodeState();
                try {
                    activity.handleOcrContinuousDecode((OcrResultFailure) message.obj);
                } catch (NullPointerException e) {
                    Log.w(TAG, "got bad OcrResultFailure", e);
                }
                if (state == State.CONTINUOUS) {
                    restartOcrPreviewAndDecode();
                }
                break;
            case Constant.CJ_OCR_CONTINUOUS_DECODE_SUCCEEDED:
                DecodeHandler.resetDecodeState();
                try {
                    activity.handleOcrContinuousDecode((OcrResultBean) message.obj);
                } catch (NullPointerException e) {
                    // Continue
                }
                if (state == State.CONTINUOUS) {
                    restartOcrPreviewAndDecode();
                }
                break;
            case Constant.CJ_OCR_DECODE_SUCCEEDED:
                state = State.SUCCESS;
                activity.setShutterButtonClickable(true);
                activity.handleOcrDecode((OcrResultBean) message.obj);
                break;
            case Constant.CJ_OCR_DECODE_FAILED:
                state = State.PREVIEW;
                activity.setShutterButtonClickable(true);
                Toast toast = Toast.makeText(activity.getBaseContext(), "OCR failed. Please try again.", Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
                break;
        }
    }

    void stop() {
        // TODO See if this should be done by sending a quit message to decodeHandler as is done
        // below in quitSynchronously().

        Log.d(TAG, "Setting state to CONTINUOUS_PAUSED.");
        state = State.CONTINUOUS_PAUSED;
        removeMessages(Constant.CJ_OCR_CONTINUOUS_DECODE);
        removeMessages(Constant.CJ_OCR_DECODE);
        removeMessages(Constant.CJ_OCR_CONTINUOUS_DECODE_FAILED);
        removeMessages(Constant.CJ_OCR_CONTINUOUS_DECODE_SUCCEEDED); // TODO are these removeMessages() calls doing anything?

        // Freeze the view displayed to the user.
//    CameraManager.get().stopPreview();
    }

    void resetState() {
        //Log.d(TAG, "in restart()");
        if (state == State.CONTINUOUS_PAUSED) {
            Log.d(TAG, "Setting state to CONTINUOUS");
            state = State.CONTINUOUS;
            restartOcrPreviewAndDecode();
        }
    }

    void quitSynchronously() {
        state = State.DONE;
        if (cameraManager != null) {
            cameraManager.stopPreview();
        }
        //Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit);
        try {
            //quit.sendToTarget(); // This always gives "sending message to a Handler on a dead thread"

            // Wait at most half a second; should be enough time, and onPause() will timeout quickly
            decodeThread.join(500L);
        } catch (InterruptedException e) {
            Log.w(TAG, "Caught InterruptedException in quitSyncronously()", e);
            // continue
        } catch (RuntimeException e) {
            Log.w(TAG, "Caught RuntimeException in quitSyncronously()", e);
            // continue
        } catch (Exception e) {
            Log.w(TAG, "Caught unknown Exception in quitSynchronously()", e);
        }

        // Be absolutely sure we don't send any queued up messages
        removeMessages(Constant.CJ_OCR_CONTINUOUS_DECODE);
        removeMessages(Constant.CJ_OCR_DECODE);

    }

    /**
     *  Start the preview, but don't try to OCR anything until the user presses the shutter button.
     */
    private void restartOcrPreview() {
        // Display the shutter and torch buttons
        activity.setButtonVisibility(true);

        if (state == State.SUCCESS) {
            state = State.PREVIEW;

            // Draw the viewfinder.
            activity.drawViewfinder();
        }
    }

    /**
     *  Send a decode request for realtime OCR mode
     */
    private void restartOcrPreviewAndDecode() {
        // Continue capturing camera frames
        cameraManager.startPreview();

        // Continue requesting decode of images
        cameraManager.requestOcrDecode(decodeThread.getHandler(), Constant.CJ_OCR_CONTINUOUS_DECODE);
        activity.drawViewfinder();
    }

    /**
     * Request OCR on the current preview frame.
     */
    private void ocrDecode() {
        state = State.PREVIEW_PAUSED;
        cameraManager.requestOcrDecode(decodeThread.getHandler(), Constant.CJ_OCR_DECODE);
    }

    /**
     * Request OCR when the hardware shutter button is clicked.
     */
    void hardwareShutterButtonClick() {
        // Ensure that we're not in continuous recognition mode
        if (state == State.PREVIEW) {
            ocrDecode();
        }
    }

    /**
     * Request OCR when the on-screen shutter button is clicked.
     */
    void shutterButtonClick() {
        // Disable further clicks on this button until OCR request is finished
        activity.setShutterButtonClickable(false);
        ocrDecode();
    }

}
