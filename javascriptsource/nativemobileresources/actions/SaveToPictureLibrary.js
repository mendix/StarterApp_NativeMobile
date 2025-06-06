import { CameraRoll } from '@react-native-camera-roll/camera-roll';

// This file was generated by Mendix Studio Pro.
// BEGIN EXTRA CODE
// END EXTRA CODE
/**
 * @param {MxObject} picture - This field is required.
 * @returns {Promise.<string>}
 */
async function SaveToPictureLibrary(picture) {
    // BEGIN USER CODE
    // Documentation https://facebook.github.io/react-native/docs/cameraroll#savetocameraroll
    if (!picture) {
        return Promise.reject(new Error("Input parameter 'Picture' is required"));
    }
    if (!picture.inheritsFrom("System.FileDocument")) {
        const entity = picture.getEntity();
        return Promise.reject(new Error(`Entity ${entity} does not inherit from 'System.FileDocument'`));
    }
    const guid = picture.getGuid();
    const changedDate = picture.get("changedDate");
    const url = mx.data.getDocumentUrl(guid, changedDate);
    // Save the file as a photo to the camera roll.
    try {
        const savedUri = await CameraRoll.saveToCameraRoll(url, "auto");
        return Promise.resolve(savedUri.node.image.uri);
    }
    catch (error) {
        return Promise.reject(error);
    }
    // END USER CODE
}

export { SaveToPictureLibrary };
