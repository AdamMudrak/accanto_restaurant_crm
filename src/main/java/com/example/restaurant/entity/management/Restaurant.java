package com.example.restaurant.entity.management;

import com.example.restaurant.entity.image.Image;
import com.example.restaurant.service.payment.PaymentProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logo_id")
    private Image logo;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_image_id")
    private Image backgroundImage;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DefaultRegion defaultRegion = DefaultRegion.UKRAINE;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language = Language.UA;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency = Currency.UAH;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProviderType paymentProvider = PaymentProviderType.MONOBANK;

    public enum DefaultRegion {
        EUROPE,
        UKRAINE
    }

    public enum Language {
        UA,
        EN
    }

    public enum Currency {
        UAH,
        EUR
    }
}
