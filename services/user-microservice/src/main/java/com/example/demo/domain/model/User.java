package com.example.demo.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(unique = true, nullable = false)
  private String email;

  @JsonIgnore
  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  public enum UserRole {
      ADMIN,
      USER
  }

  public void changePassword(String newPassword, PasswordEncoder passwordEncoder) {
      if(newPassword == null || newPassword.trim().isEmpty()){
          throw new IllegalArgumentException("New password cannot be empty");
      }
      // bỏ khoảng trắng đầu cuối
      newPassword = newPassword.trim();

      // kt độ mạnh mật khẩu bằng regex
      if (!isStrongPassword(newPassword)) {
          throw new IllegalArgumentException(
                  "Password must be at least 8 characters long, contain upper and lower case letters, a number, and a special character."
          );
      }

      this.password = passwordEncoder.encode(newPassword);
  }

    // Hàm riêng để kiểm tra độ mạnh mật khẩu
    private boolean isStrongPassword(String password) {
        // Regex: ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
    }
}
