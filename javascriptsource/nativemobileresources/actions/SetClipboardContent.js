import { Clipboard } from 'react-native';

// This file was generated by Mendix Studio Pro.
// BEGIN EXTRA CODE
// END EXTRA CODE
/**
 * @param {string} content - This field is required.
 * @returns {Promise.<void>}
 */
async function SetClipboardContent(content) {
    // BEGIN USER CODE
    // Documentation https://facebook.github.io/react-native/docs/clipboard#setstring
    if (!content) {
        return Promise.reject(new Error("Input parameter 'Content' is required"));
    }
    Clipboard.setString(content);
    return Promise.resolve();
    // END USER CODE
}

export { SetClipboardContent };
