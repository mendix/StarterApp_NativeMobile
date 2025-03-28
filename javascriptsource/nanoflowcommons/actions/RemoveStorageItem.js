import AsyncStorage from '@react-native-async-storage/async-storage';

// This file was generated by Mendix Studio Pro.
// BEGIN EXTRA CODE
// END EXTRA CODE
/**
 * Remove a content identified by a unique key. This could be set via any of the Set Storage Item JavaScript actions.
 * @returns {Promise.<boolean>}
 */
async function RemoveStorageItem(key) {
    // BEGIN USER CODE
    if (!key) {
        return Promise.reject(new Error("Input parameter 'Key' is required"));
    }
    return removeItem(key).then(() => true);
    function removeItem(key) {
        if (navigator && navigator.product === "ReactNative") {
            return AsyncStorage.removeItem(key);
        }
        if (window) {
            window.localStorage.removeItem(key);
            return Promise.resolve();
        }
        return Promise.reject(new Error("No storage API available"));
    }
    // END USER CODE
}

export { RemoveStorageItem };
