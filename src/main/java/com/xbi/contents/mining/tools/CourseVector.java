package com.xbi.contents.mining.tools;

import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * Created by usr0101862 on 2016/08/06.
 */
public class CourseVector {
    private String courseId;
    private INDArray vectors;

    public CourseVector(String courseId) {
        this.courseId = courseId;
    }

    public CourseVector(String courseId, INDArray vectors) {
        this.courseId = courseId;
        this.vectors = vectors;
    }


    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public INDArray getVectors() {
        return vectors;
    }

    public void setVectors(INDArray vectors) {
        this.vectors = vectors;
    }
}
