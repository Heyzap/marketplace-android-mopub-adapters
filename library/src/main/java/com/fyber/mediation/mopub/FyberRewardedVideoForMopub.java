/** Copyright 2020 Fyber N.V.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License
 */

package com.fyber.mediation.mopub;

import android.app.Activity;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.fyber.inneractive.sdk.external.*;
import com.fyber.inneractive.sdk.external.InneractiveUnitController.AdDisplayError;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventRewardedVideo;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoManager;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;

/**
 * Implements Fyber's rewarded video Mopub's custom event class
 */
public class FyberRewardedVideoForMopub extends CustomEventRewardedVideo {
    // Mopub log tag definition
    private final static String LOG_TAG = "FyberRewardedVideoForMopub";

    /** Cache the spot id, and return when getAdNetworkId is called.
     * Initialized to "", because getAdNetworkId cannot return null */
    private String mSpotId = "";

    InneractiveAdSpot mRewardedSpot;
    Activity mParentActivity;
    private boolean mRewarded = false;

    /**
     * Not implemented. The Fyber SDK listens to lifecycle events internally
     * @return
     */
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    /**
     * Returns Fyber's spot id, as a unique identifier for the ad network id
     * @return
     */
    @Override
    protected String getAdNetworkId() {
        return mSpotId;
    }

    @Override
    protected void onInvalidate() {
        if (mRewardedSpot != null) {
            mRewardedSpot.destroy();
            mRewardedSpot = null;
        }
    }

    /**
     * Not implemented. Called by Mopub in order to initialize the SDK.
     * The Fyber SDK should be implemented via the adapter configuration class {@link FyberAdapterConfiguration}
     * @param launcherActivity
     * @param localExtras
     * @param serverExtras
     * @return
     * @throws Exception
     */
    @Override
    protected boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception {

        return true;
    }

    /**
     * Called in order to load a rewarded video ad. The Spot and app id, should be defined within on the mopub console
     * In order to get them via the serverExtras parameter
     * @param activity parent activity
     * @param localExtras support the keys which are defined in {@link InneractiveMediationDefs}
     * @param serverExtras should be populate with app and spot ids
     * @throws Exception
     */
    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception {
        log("load rewarded requested");

        String appId = null;

        if (serverExtras != null) {
            log("server extras: " + serverExtras);
            appId = serverExtras.get(FyberMopubMediationDefs.REMOTE_KEY_APP_ID);
            mSpotId = serverExtras.get(InneractiveMediationDefs.REMOTE_KEY_SPOT_ID);
        }

        if (TextUtils.isEmpty(mSpotId)) {
            log("No spotID defined for ad unit. Cannot load rewarded video");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FyberRewardedVideoForMopub.class, "", MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        // If we've received an appId for this unit, try initializing the Fyber Marketplace SDK, if it was not already initialized
        if (!TextUtils.isEmpty(appId)) {
            FyberAdapterConfiguration.initializeFyberMarketplace(activity, appId, serverExtras.containsKey(FyberMopubMediationDefs.REMOTE_KEY_DEBUG));
        }

        InneractiveUserConfig.Gender gender = null;
        int age = 0;
        String zipCode = null;
        String keywords = null;
        if (localExtras != null) {
            
			/* Set keywords variable as defined on MoPub console, you can also define keywords with setlocalExtras in IaMediationActivity class.
			in case the variable is not initialized, the variable will not be in use */
            if (localExtras.containsKey(InneractiveMediationDefs.KEY_KEYWORDS)) {
                keywords = (String) localExtras.get(InneractiveMediationDefs.KEY_KEYWORDS);
            }
            
			/* Set the age variable as defined on IaMediationActivity class.   
			in case the variable is not initialized, the variable will not be in use */
            if (localExtras.containsKey(InneractiveMediationDefs.KEY_AGE)) {
                try {
                    age = Integer.valueOf(localExtras.get(InneractiveMediationDefs.KEY_AGE).toString());
                } catch (NumberFormatException e) {
                    log("local extra contains Invalid Age");
                }
            }

			/* Set the gender variable as defined on IaMediationActivity class.   
			in case the variable is not initialized, the variable will not be in use */
            if (localExtras.containsKey(InneractiveMediationDefs.KEY_GENDER)) {
                String genderStr = localExtras.get(InneractiveMediationDefs.KEY_GENDER).toString();
                if (genderStr.equals(InneractiveMediationDefs.GENDER_MALE)) {
                    gender = InneractiveUserConfig.Gender.MALE;
                } else if (genderStr.equals(InneractiveMediationDefs.GENDER_FEMALE)) {
                    gender = (InneractiveUserConfig.Gender.FEMALE);
                }
            }

			/* Set zipCode variable as defined on IaMediationActivity class.   
			in case the variable is not initialized, the variable will not be in use */
            if (localExtras.containsKey(InneractiveMediationDefs.KEY_ZIPCODE)) {
                zipCode = (String) localExtras.get(InneractiveMediationDefs.KEY_ZIPCODE);
            }
        }

        if (mRewardedSpot != null) {
            mRewardedSpot.destroy();
        }

        mRewardedSpot = InneractiveAdSpotManager.get().createSpot();
        // Set your mediation name and version
        mRewardedSpot.setMediationName(InneractiveMediationName.MOPUB);
        mRewardedSpot.setMediationVersion(MoPub.SDK_VERSION);

        InneractiveFullscreenUnitController fullscreenUnitController = new InneractiveFullscreenUnitController();
        mRewardedSpot.addUnitController(fullscreenUnitController);

        InneractiveAdRequest request = new InneractiveAdRequest(mSpotId);
        request.setUserParams( new InneractiveUserConfig()
                .setGender(gender)
                .setZipCode(zipCode)
                .setAge(age));
        if (!TextUtils.isEmpty(keywords)) {
            request.setKeywords(keywords);
        }

        // Load ad
        mRewardedSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {

            /**
             * Called by Inneractive when an interstitial is ready for display
             * @param adSpot Spot object
             */
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                log("on ad loaded successfully");
                MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(FyberRewardedVideoForMopub.class, mSpotId);
            }

            /**
             * Called by Inneractive an interstitial fails loading
             * @param adSpot Spot object
             * @param errorCode the failure's error.
             */
            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot, InneractiveErrorCode errorCode) {
                log("Failed loading rewarded with error: " + errorCode);
                if (errorCode == InneractiveErrorCode.CONNECTION_ERROR) {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FyberRewardedVideoForMopub.class, mSpotId, MoPubErrorCode.NO_CONNECTION);
                } else if  (errorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FyberRewardedVideoForMopub.class, mSpotId, MoPubErrorCode.NETWORK_TIMEOUT);
                } else if (errorCode == InneractiveErrorCode.NO_FILL) {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FyberRewardedVideoForMopub.class, mSpotId, MoPubErrorCode.NO_FILL);
                } else {
                    MoPubRewardedVideoManager.onRewardedVideoLoadFailure(FyberRewardedVideoForMopub.class, mSpotId, MoPubErrorCode.SERVER_ERROR);
                }
            }
        });

        mRewardedSpot.requestAd(request);

        mParentActivity = activity;
    }

    /**
     * Called by Mopub, in order to check if there is a rewarded ad ready for display
     * @return
     */
    @Override
    protected boolean hasVideoAvailable() {
        return mRewardedSpot != null && mRewardedSpot.isReady();
    }

    /**
     * Called by Mopub in order to show a standalone activity of a rewarded video ad
     */
    @Override
    protected void showVideo() {
        log("showVideo called for rewarded");
        // check if the ad is ready
        if (mRewardedSpot != null && mRewardedSpot.isReady()) {

            InneractiveFullscreenUnitController fullscreenUnitController = (InneractiveFullscreenUnitController)mRewardedSpot.getSelectedUnitController();
            fullscreenUnitController.setEventsListener(new InneractiveFullscreenAdEventsListener() {

                /**
                 * Called by Inneractive when an interstitial ad activity is closed
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdDismissed(InneractiveAdSpot adSpot) {
                    log("onAdDismissed");
                    MoPubRewardedVideoManager.onRewardedVideoCompleted(FyberRewardedVideoForMopub.class, mSpotId, mRewarded ? MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.DEFAULT_REWARD_AMOUNT) : MoPubReward.failure());
                    MoPubRewardedVideoManager.onRewardedVideoClosed(FyberRewardedVideoForMopub.class, mSpotId);
                }

                /**
                 * Called by Inneractive when an interstitial ad activity is shown
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdImpression(InneractiveAdSpot adSpot) {
                    log("onAdImpression");
                    MoPubRewardedVideoManager.onRewardedVideoStarted(FyberRewardedVideoForMopub.class, mSpotId);
                }

                /**
                 * Called by Inneractive when an interstitial ad is clicked
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdClicked(InneractiveAdSpot adSpot) {
                    MoPubRewardedVideoManager.onRewardedVideoClicked(FyberRewardedVideoForMopub.class, mSpotId);
                    log("onAdClicked");
                }

                /**
                 * Called by Inneractive when an interstitial ad opened an external application
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
                    log("onAdWillOpenExternalApp");
                    // Don't call the onLeaveApplication() API since it causes a false Click event on MoPub
                }

                /**
                 * Called when an ad has entered an error state, this will only happen when the ad is being shown
                 * @param adSpot the relevant ad spot
                 */
                @Override
                public void onAdEnteredErrorState(InneractiveAdSpot adSpot, AdDisplayError error) {
                    log("onAdEnteredErrorState - " + error.getMessage());
                }

                /**
                 * Called by Inneractive when Inneractive's internal browser, which was opened by this interstitial, was closed
                 * @param adSpot Spot object
                 */
                @Override
                public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
                    log("onAdWillCloseInternalBrowser");
                }
            });

            // Add video content controller, for controlling video ads
            InneractiveFullscreenVideoContentController videoContentController = new InneractiveFullscreenVideoContentController();
            videoContentController.setEventsListener(new VideoContentListener() {
                @Override
                public void onProgress(int totalDurationInMsec, int positionInMsec) {
                        // Nothing to do here
                }

                /**
                 * Called by inneractive when an Intersititial video ad was played to the end
                 * <br>Can be used for incentive flow
                 * <br>Note: This event does not indicate that the interstitial was closed
                 */
                @Override
                public void onCompleted() {
                    mRewarded = true;
                    log("Got video content completed event");
                }

                @Override
                public void onPlayerError() {
                    log("Got video content play error event");
                    MoPubRewardedVideoManager.onRewardedVideoPlaybackError(FyberRewardedVideoForMopub.class, mSpotId, MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
                }
            });

            // Now add the content controller to the unit controller
            fullscreenUnitController.addContentController(videoContentController);

            fullscreenUnitController.show(mParentActivity);
        } else {
            log("The rewarded ad is not ready yet.");
        }
    }

    /**
     * MopubLog helper
     * @param message
     */
    private void log(String message) {
        MoPubLog.log(CUSTOM, LOG_TAG, message);
    }
}
