// RM TUITION CENTER - Shared Utility Functions

const TOKEN_KEY = 'rm_tuition_jwt';
const USER_KEY = 'rm_tuition_user';

// Class standard display mapping
const CLASS_MAPPING = {
  '10': '10th Standard',
  '11-all': '11th – All Subjects',
  '11-pcm': '11th – PCM',
  '11-pc': '11th – PC',
  '11-maths': '11th – Maths Only',
  '12-all': '12th – All Subjects',
  '12-pcm': '12th – PCM',
  '12-pc': '12th – PC',
  '12-maths': '12th – Maths Only'
};

// Check authentication
function checkAuth(requiredRole) {
  const token = localStorage.getItem(TOKEN_KEY);
  const userStr = localStorage.getItem(USER_KEY);
  
  if (!token || !userStr) {
    logout();
    return null;
  }
  
  const user = JSON.parse(userStr);
  if (requiredRole && user.role !== requiredRole) {
    // Redirect role mismatch
    if (user.role === 'ROLE_ADMIN') {
      window.location.href = '/admin/dashboard.html';
    } else {
      window.location.href = '/student/dashboard.html';
    }
    return null;
  }
  
  return { token, user };
}

// Fetch helper with JWT header
async function fetchAPI(url, options = {}) {
  const token = localStorage.getItem(TOKEN_KEY);
  
  const headers = {
    ...options.headers,
  };
  
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  
  if (options.body && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
  }
  
  const response = await fetch(url, {
    ...options,
    headers
  });
  
  if (response.status === 401 || response.status === 403) {
    // Session expired or access denied
    logout();
    return null;
  }
  
  return response;
}

// Log out user
function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = '/login.html';
}

// Format Date
function formatDate(dateString) {
  if (!dateString) return '-';
  const d = new Date(dateString);
  return d.toLocaleString('en-IN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });
}

// Display inline error or success alerts
function showAlert(message, isError = true, containerId = 'alert-container') {
  const container = document.getElementById(containerId);
  if (!container) return;
  
  container.innerHTML = `
    <div style="
      padding: 10px 15px; 
      border-radius: 8px; 
      margin-bottom: 15px; 
      font-size: 0.9rem;
      font-weight: 500;
      background: ${isError ? 'rgba(239, 68, 68, 0.15)' : 'rgba(16, 185, 129, 0.15)'};
      border: 1px solid ${isError ? 'rgba(239, 68, 68, 0.3)' : 'rgba(16, 185, 129, 0.3)'};
      color: ${isError ? '#f87171' : '#34d399'};
    ">
      ${message}
    </div>
  `;
}

// Initialize common page logic: sidebar active status, topbar profile badges, and logout listener
document.addEventListener('DOMContentLoaded', () => {
  const token = localStorage.getItem(TOKEN_KEY);
  const userStr = localStorage.getItem(USER_KEY);
  
  if (token && userStr) {
    const user = JSON.parse(userStr);
    
    // Inject profile info in badges
    const usernameBadge = document.querySelector('.user-profile-badge span');
    if (usernameBadge) {
      usernameBadge.textContent = user.name || user.email;
    }
    
    const avatarBadge = document.querySelector('.user-profile-badge img');
    if (avatarBadge) {
      if (user.avatarUrl && user.avatarUrl.trim() !== '') {
        avatarBadge.src = user.avatarUrl;
      } else {
        avatarBadge.src = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200';
      }
    }
  }
  
  // Connect logout trigger
  const logoutBtn = document.getElementById('logout-btn');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
      e.preventDefault();
      logout();
    });
  }
  
  // Mobile Nav drawer events
  const mobileToggle = document.getElementById('mobile-nav-toggle');
  const sidebar = document.querySelector('.sidebar');
  if (mobileToggle && sidebar) {
    mobileToggle.addEventListener('click', () => {
      sidebar.classList.toggle('mobile-active');
    });
    
    // Close sidebar on background click
    document.addEventListener('click', (e) => {
      if (!sidebar.contains(e.target) && !mobileToggle.contains(e.target) && sidebar.classList.contains('mobile-active')) {
        sidebar.classList.remove('mobile-active');
      }
    });
  }
});
