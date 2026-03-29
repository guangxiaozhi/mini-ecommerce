package com.guang.miniecommercebackend.config;

import com.guang.miniecommercebackend.entity.Product;
import com.guang.miniecommercebackend.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 仅在 local 配置下：若库里还没有商品，则插入几条示例，方便你第 6 步直接 GET 验证。
 * 不需要时删掉本类或改 profile 即可。
 */
@Component
@Profile("local")
public class ProductSampleDataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    public ProductSampleDataLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        Product p1 = new Product();
        p1.setName("示例笔记本");
        p1.setDescription("入门学习用笔记本，示例数据。");
        p1.setPrice(new BigDecimal("29.99"));
        p1.setStock(100);
        p1.setActive(true);

        Product p2 = new Product();
        p2.setName("示例鼠标");
        p2.setDescription("无线鼠标，示例数据。");
        p2.setPrice(new BigDecimal("15.50"));
        p2.setStock(50);
        p2.setActive(true);

        productRepository.save(p1);
        productRepository.save(p2);
    }
}
