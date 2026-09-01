package com.mall.search.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.search.entity.EsProduct;
import com.mall.search.entity.Product;
import com.mall.search.mapper.ProductMapper;
import com.mall.search.repository.EsProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSearchService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private EsProductRepository esProductRepository;

    /**
     * 搜索商品
     */
    public List<EsProduct> searchProducts(String keyword) {
        // 使用 Criteria API 构建查询，避免 JSON 拼接注入风险
        Criteria criteria = new Criteria("name").matches(keyword)
                .or("brand").matches(keyword)
                .or("description").matches(keyword);
        CriteriaQuery query = new CriteriaQuery(criteria);

        SearchHits<EsProduct> searchHits = elasticsearchOperations.search(query, EsProduct.class);
        return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * 按分类搜索商品
     */
    public List<EsProduct> searchByCategory(Long categoryId) {
//        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.eq(Product::getCategoryId, categoryId);
//
//        return productMapper.selectList(queryWrapper);
        return esProductRepository.findByCategoryId(categoryId);
    }

    /**
     * 按品牌搜索商品
     */
    public List<EsProduct> searchByBrand(String brand) {
//        LambdaQueryWrapper<Product> queryWrapper = new LambdaQueryWrapper<>();
//        queryWrapper.like(Product::getBrand, brand);
//
//        return productMapper.selectList(queryWrapper);
        return esProductRepository.findByBrand(brand);
    }

    /**
     * 全量导入商品数据到 ES（从 DB 同步）
     */
    public int importAll() {
        List<Product> products = productMapper.selectList(null);
        List<EsProduct> esProducts = products.stream().map(p -> {
            EsProduct es = new EsProduct();
            es.setId(p.getId());
            es.setName(p.getName());
            es.setCategoryId(p.getCategoryId());
            es.setBrand(p.getBrand());
            es.setDescription(p.getDescription());
            es.setPrice(p.getPrice());
            es.setMainImage(p.getMainImage());
            es.setSales(p.getSales());
            return es;
        }).collect(Collectors.toList());
        esProductRepository.saveAll(esProducts);
        log.info("导入 {} 条商品到 ES", esProducts.size());
        return esProducts.size();
    }
}