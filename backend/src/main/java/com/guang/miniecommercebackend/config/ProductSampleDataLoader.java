package com.guang.miniecommercebackend.config;

import com.guang.miniecommercebackend.entity.Product;
import com.guang.miniecommercebackend.entity.ProductImage;
import com.guang.miniecommercebackend.entity.ProductBullet;
import com.guang.miniecommercebackend.entity.ShippingOption;
import com.guang.miniecommercebackend.repository.ProductRepository;
import com.guang.miniecommercebackend.repository.ProductImageRepository;
import com.guang.miniecommercebackend.repository.ProductBulletRepository;
import com.guang.miniecommercebackend.repository.ShippingOptionRepository;
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
    private final ProductImageRepository productImageRepository;
    private final ProductBulletRepository productBulletRepository;
    private final ShippingOptionRepository shippingOptionRepository;

    public ProductSampleDataLoader(ProductRepository productRepository,
                                   ProductImageRepository productImageRepository,
                                   ProductBulletRepository productBulletRepository,
                                   ShippingOptionRepository shippingOptionRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productBulletRepository = productBulletRepository;
        this.shippingOptionRepository = shippingOptionRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }

        // ── Product 1 ──
        Product p1 = new Product();
        p1.setName("示例笔记本");
        p1.setDescription("入门学习用笔记本，示例数据。");
        p1.setPrice(new BigDecimal("29.99"));
        p1.setStock(100);
        p1.setActive(true);
        productRepository.save(p1);

        // image
        ProductImage img1 = new ProductImage();
        img1.setProduct(p1);
        img1.setImageUrl("https://plus.unsplash.com/premium_photo-1680539292648-e03dfae3e345?q=80&w=774&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D");
        img1.setIsPrimary(true);
        img1.setSortOrder(0);
        productImageRepository.save(img1);

        // highlights row
        ProductBullet b1Highlights = new ProductBullet();
        b1Highlights.setProduct(p1);
        b1Highlights.setBrand("示例品牌");
        b1Highlights.setWeight("500 grams");
        b1Highlights.setDimension("21 x 14.8 x 1.5 cm");
        b1Highlights.setSortOrder(0);
        productBulletRepository.save(b1Highlights);

        // bullet points
        ProductBullet b1Point1 = new ProductBullet();
        b1Point1.setProduct(p1);
        b1Point1.setContent("200 pages of high-quality lined paper");
        b1Point1.setSortOrder(1);
        productBulletRepository.save(b1Point1);

        ProductBullet b1Point2 = new ProductBullet();
        b1Point2.setProduct(p1);
        b1Point2.setContent("Durable hardcover suitable for daily use");
        b1Point2.setSortOrder(2);
        productBulletRepository.save(b1Point2);

        // shipping
        ShippingOption s1 = new ShippingOption();
        s1.setProduct(p1);
        s1.setLabel("FREE delivery");
        s1.setDescription("Estimated 3-5 business days");
        s1.setIsFree(true);
        shippingOptionRepository.save(s1);

        // ── Product 2 ──
        Product p2 = new Product();
        p2.setName("示例鼠标");
        p2.setDescription("无线鼠标，示例数据。");
        p2.setPrice(new BigDecimal("15.50"));
        p2.setStock(50);
        p2.setActive(true);
        productRepository.save(p2);

        ProductImage img2 = new ProductImage();
        img2.setProduct(p2);
        img2.setImageUrl("https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?q=80&w=1065&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D");
        img2.setIsPrimary(true);
        img2.setSortOrder(0);
        productImageRepository.save(img2);

        ProductBullet b2Highlights = new ProductBullet();
        b2Highlights.setProduct(p2);
        b2Highlights.setBrand("示例品牌");
        b2Highlights.setWeight("120 grams");
        b2Highlights.setDimension("11.5 x 6.2 x 3.8 cm");
        b2Highlights.setSortOrder(0);
        productBulletRepository.save(b2Highlights);

        ProductBullet b2Point1 = new ProductBullet();
        b2Point1.setProduct(p2);
        b2Point1.setContent("Wireless 2.4GHz with USB receiver");
        b2Point1.setSortOrder(1);
        productBulletRepository.save(b2Point1);

        ShippingOption s2 = new ShippingOption();
        s2.setProduct(p2);
        s2.setLabel("FREE delivery Overnight");
        s2.setDescription("4 AM - 8 AM on qualifying orders over $25");
        s2.setIsFree(true);
        shippingOptionRepository.save(s2);

        productRepository.save(p1);
        productRepository.save(p2);

        // ── Product 3 ──
        Product p3 = new Product();
        p3.setName("Table");
        p3.setDescription("A sturdy wooden table, perfect for home or office use.");
        p3.setPrice(new BigDecimal("48.00"));
        p3.setStock(1);
        p3.setActive(true);
        productRepository.save(p3);

        // image
        ProductImage img3 = new ProductImage();
        img3.setProduct(p3);
        img3.setImageUrl("https://images.unsplash.com/photo-1623177623442-979c1e42c255?q=80&w=987&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D");  // ← paste your URL here
        img3.setIsPrimary(true);
        img3.setSortOrder(0);
        productImageRepository.save(img3);

        // highlights
        ProductBullet b3Highlights = new ProductBullet();
        b3Highlights.setProduct(p3);
        b3Highlights.setBrand("示例品牌");
        b3Highlights.setWeight("15 kg");
        b3Highlights.setDimension("120 x 60 x 75 cm");
        b3Highlights.setSortOrder(0);
        productBulletRepository.save(b3Highlights);

        // bullet points
        ProductBullet b3Point1 = new ProductBullet();
        b3Point1.setProduct(p3);
        b3Point1.setContent("Solid wood construction for durability");
        b3Point1.setSortOrder(1);
        productBulletRepository.save(b3Point1);

        ProductBullet b3Point2 = new ProductBullet();
        b3Point2.setProduct(p3);
        b3Point2.setContent("Easy to assemble with included instructions");
        b3Point2.setSortOrder(2);
        productBulletRepository.save(b3Point2);

        // shipping
        ShippingOption s3 = new ShippingOption();
        s3.setProduct(p3);
        s3.setLabel("FREE delivery");
        s3.setDescription("Estimated 5-7 business days");
        s3.setIsFree(true);
        shippingOptionRepository.save(s3);

        productRepository.save(p3);
    }
}
