package org.olgakhamzina.scientificlibrarythesis.presentation.detail

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

actual fun openPdf(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        val safariVC = SFSafariViewController(nsUrl)
        val rootVC: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootVC?.presentViewController(safariVC, animated = true, completion = null)
    }
}