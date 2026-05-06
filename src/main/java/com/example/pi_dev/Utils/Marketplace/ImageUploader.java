package com.example.pi_dev.Utils.Marketplace;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ImageUploader {

    public static String uploadImage(File file) {
        Cloudinary cloudinary = CloudinaryConfig.getCloudinary();
        try {
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());
            // Get the URL of the uploaded image
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
