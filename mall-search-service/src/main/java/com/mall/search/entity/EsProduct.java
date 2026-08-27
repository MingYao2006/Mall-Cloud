package com.mall.search.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;

@Data
@Document(indexName = "product")
public class EsProduct {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String name;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)
    private String mainImage;

    @Field(type = FieldType.Integer)
    private Integer sales;
}