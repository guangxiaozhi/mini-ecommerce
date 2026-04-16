package com.guang.miniecommercebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class UserAddressRequest {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 50, message = "Receiver name must not exceed 50 characters")
    private String receiverName;

    @NotBlank(message = "Receiver phone is required")
    @Size(max = 20, message = "Receiver phone must not exceed 20 characters")
    @Pattern(regexp = "^[0-9+\\-() ]*$", message = "Phone number contains invalid characters")
    private String receiverPhone;

    @NotBlank(message = "State is required")
    @Size(max = 50, message = "State must not exceed 50 characters")
    private String state;

    @NotBlank(message = "City is required")
    @Size(max = 50, message = "City name must not exceed 50 characters")
    private String city;

    @Size(max = 50, message = "District must not exceed 50 characters")
    private String district;

    @NotBlank(message = "Detail address is required")
    @Size(max = 200, message = "Detail address must not exceed 200 characters")
    private String detailAddress;

    private Boolean isDefault = false;

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getDetailAddress() { return detailAddress; }
    public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}

