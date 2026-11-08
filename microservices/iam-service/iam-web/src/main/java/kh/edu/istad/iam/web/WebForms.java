package kh.edu.istad.iam.web;

import java.time.LocalDate;

public final class WebForms {
    private WebForms() { }

    public static class Registration {
        private String username, email, password, confirmPassword, givenName, familyName, locale;
        public String getUsername() { return username; }
        public void setUsername(String value) { username = value; }
        public String getEmail() { return email; }
        public void setEmail(String value) { email = value; }
        public String getPassword() { return password; }
        public void setPassword(String value) { password = value; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String value) { confirmPassword = value; }
        public String getGivenName() { return givenName; }
        public void setGivenName(String value) { givenName = value; }
        public String getFamilyName() { return familyName; }
        public void setFamilyName(String value) { familyName = value; }
        public String getLocale() { return locale == null ? "en" : locale; }
        public void setLocale(String value) { locale = value; }
    }

    public static class Profile {
        private String givenName, familyName, middleName, nickname, gender, locale,
                profileUrl, pictureUrl, websiteUrl, phoneNumber, zoneinfo,
                addressStreetAddress, addressLocality, addressRegion, addressPostalCode, addressCountry;
        private LocalDate birthdate;
        public String getGivenName() { return givenName; } public void setGivenName(String v) { givenName = v; }
        public String getFamilyName() { return familyName; } public void setFamilyName(String v) { familyName = v; }
        public String getMiddleName() { return middleName; } public void setMiddleName(String v) { middleName = v; }
        public String getNickname() { return nickname; } public void setNickname(String v) { nickname = v; }
        public String getGender() { return gender; } public void setGender(String v) { gender = v; }
        public String getLocale() { return locale; } public void setLocale(String v) { locale = v; }
        public String getProfileUrl() { return profileUrl; } public void setProfileUrl(String v) { profileUrl = v; }
        public String getPictureUrl() { return pictureUrl; } public void setPictureUrl(String v) { pictureUrl = v; }
        public String getWebsiteUrl() { return websiteUrl; } public void setWebsiteUrl(String v) { websiteUrl = v; }
        public String getPhoneNumber() { return phoneNumber; } public void setPhoneNumber(String v) { phoneNumber = v; }
        public String getZoneinfo() { return zoneinfo; } public void setZoneinfo(String v) { zoneinfo = v; }
        public String getAddressStreetAddress() { return addressStreetAddress; } public void setAddressStreetAddress(String v) { addressStreetAddress = v; }
        public String getAddressLocality() { return addressLocality; } public void setAddressLocality(String v) { addressLocality = v; }
        public String getAddressRegion() { return addressRegion; } public void setAddressRegion(String v) { addressRegion = v; }
        public String getAddressPostalCode() { return addressPostalCode; } public void setAddressPostalCode(String v) { addressPostalCode = v; }
        public String getAddressCountry() { return addressCountry; } public void setAddressCountry(String v) { addressCountry = v; }
        public LocalDate getBirthdate() { return birthdate; } public void setBirthdate(LocalDate v) { birthdate = v; }
    }

    public static class Password {
        private String currentPassword, newPassword, confirmPassword;
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String value) { currentPassword = value; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String value) { newPassword = value; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String value) { confirmPassword = value; }
    }
}
