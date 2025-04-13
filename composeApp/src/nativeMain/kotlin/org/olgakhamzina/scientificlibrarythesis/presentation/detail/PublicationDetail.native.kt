package org.olgakhamzina.scientificlibrarythesis.presentation.detail

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

actual fun openPdf(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        // Create a SafariViewController instance with the URL
        val safariVC = SFSafariViewController(nsUrl)
        // Get the current top-most view controller.
        val rootVC: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
        // Present the SafariViewController.
        rootVC?.presentViewController(safariVC, animated = true, completion = null)
    }
}