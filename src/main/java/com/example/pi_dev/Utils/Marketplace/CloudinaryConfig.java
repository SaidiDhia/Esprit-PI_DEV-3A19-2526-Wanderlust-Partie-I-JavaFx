package com.example.pi_dev.Utils.Marketplace;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

public class CloudinaryConfig {
    private static Cloudinary cloudinary;

    public static Cloudinary getCloudinary() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "dvxulayhm",       // your cloud name
                    "api_key", "647152515138954",    // your API key
                    "api_secret", "YuYpiGSBPfEHLFj6stz7BTIqhhg", // replace with your secret
                    "secure", true
            ));
        }
        return cloudinary;
    }
}
