// RM TUITION CENTER - Authentication Scripts

document.addEventListener('DOMContentLoaded', () => {
  // 1. LOGIN FLOW
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('email').value.trim();
      const password = document.getElementById('password').value;
      
      showAlert('Logging in...', false);
      
      try {
        const response = await fetch('/api/auth/login', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password })
        });
        
        const data = await response.json();
        if (response.ok) {
          localStorage.setItem(TOKEN_KEY, data.token);
          localStorage.setItem(USER_KEY, JSON.stringify(data));
          
          showAlert('Login successful! Redirecting...', false);
          
          setTimeout(() => {
            if (data.role === 'ROLE_ADMIN') {
              window.location.href = '/admin/dashboard.html';
            } else {
              window.location.href = '/student/dashboard.html';
            }
          }, 1000);
        } else {
          showAlert(data.message || 'Login failed. Invalid credentials.');
        }
      } catch (err) {
        showAlert('Network error. Please try again.');
      }
    });
  }

  // 2. REGISTER FLOW
  const registerForm = document.getElementById('register-form');
  if (registerForm) {
    // Dynamic subject dropdown filtering based on selected class
    const classSelect = document.getElementById('standard');
    if (classSelect) {
      classSelect.addEventListener('change', () => {
        // Registering doesn't require filtering subjects, but let's confirm the class selections are correct.
      });
    }

    registerForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('name').value.trim();
      const email = document.getElementById('email').value.trim();
      const phone = document.getElementById('phone').value.trim();
      const standard = document.getElementById('standard').value;
      const password = document.getElementById('password').value;
      const confirmPassword = document.getElementById('confirm-password').value;
      
      if (password !== confirmPassword) {
        showAlert('Passwords do not match!');
        return;
      }
      
      showAlert('Registering account...', false);
      
      try {
        const response = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ name, email, phone, standard, password })
        });
        
        const data = await response.json();
        if (response.ok) {
          showAlert('Registration successful! Redirecting to login...', false);
          setTimeout(() => {
            window.location.href = '/login.html';
          }, 1500);
        } else {
          showAlert(data.message || 'Registration failed.');
        }
      } catch (err) {
        showAlert('Network error. Please try again.');
      }
    });
  }

  // 3. FORGOT PASSWORD
  const forgotForm = document.getElementById('forgot-form');
  if (forgotForm) {
    forgotForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('email').value.trim();
      
      showAlert('Processing reset request...', false);
      
      try {
        const response = await fetch('/api/auth/forgot-password', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email })
        });
        
        const data = await response.json();
        showAlert(data.message || 'Reset link processed.', false);
      } catch (err) {
        showAlert('Network error. Please try again.');
      }
    });
  }

  // 4. RESET PASSWORD
  const resetForm = document.getElementById('reset-form');
  if (resetForm) {
    resetForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const newPassword = document.getElementById('password').value;
      const confirmPassword = document.getElementById('confirm-password').value;
      
      const urlParams = new URLSearchParams(window.location.search);
      const token = urlParams.get('token');
      
      if (!token) {
        showAlert('Reset token is missing from URL!');
        return;
      }
      
      if (newPassword !== confirmPassword) {
        showAlert('Passwords do not match!');
        return;
      }
      
      showAlert('Updating password...', false);
      
      try {
        const response = await fetch('/api/auth/reset-password', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token, newPassword })
        });
        
        const data = await response.json();
        if (response.ok) {
          showAlert('Password reset successful! Redirecting to login...', false);
          setTimeout(() => {
            window.location.href = '/login.html';
          }, 1500);
        } else {
          showAlert(data.message || 'Failed to reset password.');
        }
      } catch (err) {
        showAlert('Network error. Please try again.');
      }
    });
  }
});
