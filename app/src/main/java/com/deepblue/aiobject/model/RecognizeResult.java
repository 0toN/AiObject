package com.deepblue.aiobject.model;

import java.util.List;

/**
 * @author Created by XWM on 2018-5-5.
 */
public class RecognizeResult {
    private String request_id;
    private String image_id;
    private int time_used;
    private String error_message;
    private List<Scene> scenes;
    private List<Object> objects;

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getImage_id() {
        return image_id;
    }

    public void setImage_id(String image_id) {
        this.image_id = image_id;
    }

    public int getTime_used() {
        return time_used;
    }

    public void setTime_used(int time_used) {
        this.time_used = time_used;
    }

    public String getError_message() {
        return error_message;
    }

    public void setError_message(String error_message) {
        this.error_message = error_message;
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    public void setScenes(List<Scene> scenes) {
        this.scenes = scenes;
    }

    public List<Object> getObjects() {
        return objects;
    }

    public void setObjects(List<Object> objects) {
        this.objects = objects;
    }

    @Override
    public String toString() {
        return "RecognizeResult{" +
                "request_id='" + request_id + '\'' +
                ", image_id='" + image_id + '\'' +
                ", time_used=" + time_used +
                ", error_message='" + error_message + '\'' +
                ", scenes=" + scenes +
                ", objects=" + objects +
                '}';
    }

    public class Scene {
        private String value;
        private float confidence;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }
    }

    public class Object {
        private String value;
        private float confidence;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public float getConfidence() {
            return confidence;
        }

        public void setConfidence(float confidence) {
            this.confidence = confidence;
        }
    }
}
