// RM TUITION CENTER - Student Dashboard Controller

const authInfo = checkAuth('ROLE_STUDENT');
if (!authInfo) {
  // If not student, execution stops here (redirected by checkAuth)
}

document.addEventListener('DOMContentLoaded', () => {
  if (authInfo) {
    // 1. Sidebar tab switching
    setupTabs();
    
    // 2. Personalization
    personalizeDashboard();
    
    // 3. Load sections
    loadStudentMaterials();
    loadStudentQuizzes();
    loadStudentPapers();
    loadStudentResults();
    loadStudentProfile();
    
    // Connect Form
    setupProfileListener();
  }
});

// Tab Switcher Router
function setupTabs() {
  const links = document.querySelectorAll('.sidebar-link[data-tab]');
  links.forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const tabName = link.getAttribute('data-tab');
      switchTab(tabName);
    });
  });
  
  // URL Hash check for specific tabs (e.g. #results)
  if (window.location.hash) {
    const tabName = window.location.hash.substring(1);
    if (document.getElementById(`tab-${tabName}`)) {
      switchTab(tabName);
    }
  }
}

function switchTab(tabName) {
  document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));
  const activeLink = document.querySelector(`.sidebar-link[data-tab="${tabName}"]`);
  if (activeLink) activeLink.classList.add('active');
  
  document.querySelectorAll('.tab-content').forEach(s => s.classList.add('hidden'));
  const targetSection = document.getElementById(`tab-${tabName}`);
  if (targetSection) targetSection.classList.remove('hidden');
  
  if (tabName === 'results') {
    loadStudentResults();
  } else if (tabName === 'quizzes') {
    loadStudentQuizzes();
  }
}

function personalizeDashboard() {
  const user = JSON.parse(localStorage.getItem(USER_KEY));
  document.getElementById('welcome-title').textContent = `Welcome back, ${user.name}! 👋`;
  document.getElementById('student-class-badge').textContent = `Enrolled Standard: ${CLASS_MAPPING[user.standard] || user.standard}`;
}

// Load Materials
async function loadStudentMaterials() {
  const res = await fetchAPI('/api/student/materials');
  if (res) {
    const materials = await res.json();
    const tbody = document.querySelector('#materials-table tbody');
    tbody.innerHTML = '';
    
    if (materials.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="text-center">No study materials uploaded for your class yet.</td></tr>`;
      return;
    }
    
    materials.forEach(m => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong>${m.title}</strong></td>
        <td>${m.subject}</td>
        <td>${m.fileName}</td>
        <td>${formatDate(m.uploadedAt)}</td>
        <td>
          <div class="flex gap-2">
            <button class="btn btn-secondary btn-sm" onclick="previewDoc('${m.gridFsId}', '${m.title}')">Preview</button>
            <a href="/api/files/preview/${m.gridFsId}" class="btn btn-primary btn-sm" download="${m.fileName}">Download</a>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }
}

// Load Papers
async function loadStudentPapers() {
  const res = await fetchAPI('/api/student/papers');
  if (res) {
    const papers = await res.json();
    const tbody = document.querySelector('#papers-table tbody');
    tbody.innerHTML = '';
    
    if (papers.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="text-center">No question papers uploaded for your class yet.</td></tr>`;
      return;
    }
    
    papers.forEach(p => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong>${p.title}</strong></td>
        <td>${p.subject}</td>
        <td>${p.fileName}</td>
        <td>${formatDate(p.uploadedAt)}</td>
        <td>
          <div class="flex gap-2">
            <button class="btn btn-secondary btn-sm" onclick="previewDoc('${p.gridFsId}', '${p.title}')">Preview</button>
            <a href="/api/files/preview/${p.gridFsId}" class="btn btn-primary btn-sm" download="${p.fileName}">Download</a>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }
}

// Preview Document Modal
function previewDoc(gridFsId, title) {
  // Use same-origin preview endpoint
  const url = `/api/files/preview/${gridFsId}#toolbar=0`;
  document.getElementById('preview-title').textContent = `Document Preview: ${title}`;
  document.getElementById('preview-frame').src = url;
  
  openModal('modal-preview');
}

// Load Quizzes with countdowns
let countdownIntervals = []; // Keep references to clear on refresh
async function loadStudentQuizzes() {
  // Clear any existing timer loops
  countdownIntervals.forEach(clearInterval);
  countdownIntervals = [];
  
  const res = await fetchAPI('/api/student/quizzes');
  if (res) {
    const quizzes = await res.json();
    const tbody = document.querySelector('#quizzes-table tbody');
    tbody.innerHTML = '';
    
    if (quizzes.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center">No active quizzes found.</td></tr>`;
      return;
    }
    
    quizzes.forEach((q, idx) => {
      const tr = document.createElement('tr');
      const start = q.startWindow ? new Date(q.startWindow) : null;
      const end = q.endWindow ? new Date(q.endWindow) : null;
      
      const uniqueId = `quiz-status-${idx}`;
      
      tr.innerHTML = `
        <td><strong>${q.title}</strong></td>
        <td>${q.subject}</td>
        <td>${q.questionCount} Questions</td>
        <td>${q.durationMinutes} Mins</td>
        <td style="font-size: 0.85rem;">
          Start: ${formatDate(q.startWindow)}<br>
          End: ${formatDate(q.endWindow)}
        </td>
        <td id="${uniqueId}">Checking status...</td>
      `;
      
      tbody.appendChild(tr);
      
      // Calculate and loop state timer
      updateQuizRowStatus(q, start, end, uniqueId);
    });
  }
}

function updateQuizRowStatus(quiz, start, end, elementId) {
  const container = document.getElementById(elementId);
  if (!container) return;
  
  if (quiz.attempted) {
    container.innerHTML = `<span class="badge badge-success">Attempted</span>`;
    return;
  }
  
  const timerFunc = () => {
    const now = new Date();
    
    if (start && now < start) {
      // Locked - count time left
      const diff = start.getTime() - now.getTime();
      const hrs = Math.floor(diff / (1000 * 60 * 60));
      const mins = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const secs = Math.floor((diff % (1000 * 60)) / 1000);
      
      container.innerHTML = `<span class="badge badge-warning" style="display:flex; flex-direction:column; gap:2px;">
        <span>Locked</span>
        <span style="font-size:0.7rem; font-weight:normal;">Starts in: ${hrs}h ${mins}m ${secs}s</span>
      </span>`;
    } else if (end && now > end) {
      container.innerHTML = `<span class="badge badge-danger">Expired</span>`;
    } else {
      // Open
      container.innerHTML = `
        <a href="/student/quiz-run.html?id=${quiz.id}" class="btn btn-success btn-sm">Start Quiz</a>
      `;
      // Clear countdown loop once it opens
      clearInterval(rowInterval);
    }
  };
  
  // Run once immediately
  timerFunc();
  // Set loop
  const rowInterval = setInterval(timerFunc, 1000);
  countdownIntervals.push(rowInterval);
}

// Load results reports as accordions
async function loadStudentResults() {
  const res = await fetchAPI('/api/student/results');
  if (res) {
    const grouped = await res.json();
    const container = document.getElementById('results-accordion-container');
    container.innerHTML = '';
    
    const subjects = Object.keys(grouped);
    if (subjects.length === 0) {
      container.innerHTML = `<div class="glass-card text-center"><p>No quiz attempts found. Attempt active quizzes to view results.</p></div>`;
      return;
    }
    
    subjects.forEach((subject, sIdx) => {
      const subjectSection = document.createElement('div');
      subjectSection.style.marginBottom = '1.5rem';
      
      subjectSection.innerHTML = `
        <h3 class="mb-4" style="color: var(--accent-cyan); border-bottom:1px solid var(--glass-border); padding-bottom:0.5rem;">${subject}</h3>
        <div id="subject-list-${sIdx}"></div>
      `;
      
      container.appendChild(subjectSection);
      const listContainer = document.getElementById(`subject-list-${sIdx}`);
      
      const attempts = grouped[subject];
      attempts.forEach((att, attIdx) => {
        const item = document.createElement('div');
        item.className = 'accordion-item';
        
        let headerText = `<strong>${att.quizTitle}</strong>`;
        let headerScore = '';
        
        if (att.resultsReleased) {
          headerScore = `<span style="font-weight:700; color: var(--accent-pink);">${att.score} / ${att.totalQuestions} (${att.percentage}%)</span>`;
        } else {
          headerScore = `<span class="badge badge-warning">Awaiting Results Release</span>`;
        }
        
        const accordionId = `accordion-content-${sIdx}-${attIdx}`;
        
        item.innerHTML = `
          <div class="accordion-header" onclick="toggleAccordion('${accordionId}')">
            <div>
              ${headerText}
              <div style="font-size:0.8rem; color:var(--text-secondary); margin-top:2px;">Submitted: ${formatDate(att.submittedAt)}</div>
            </div>
            <div class="flex align-center gap-3">
              ${headerScore}
              <svg style="width:16px;height:16px; transition: transform 0.3s;" class="chevron" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
            </div>
          </div>
          <div class="accordion-content" id="${accordionId}">
            ${att.resultsReleased ? renderReviewBody(att.review) : '<p class="text-center" style="color:var(--text-secondary);">The teacher has not yet released the scoring review for this quiz. Check back later!</p>'}
          </div>
        `;
        listContainer.appendChild(item);
      });
    });
  }
}

function renderReviewBody(review) {
  if (!review || review.length === 0) return '<p>No details found.</p>';
  
  return review.map((rev, idx) => {
    const isCorrect = rev.studentAnswer === rev.correctOptionIndex;
    
    return `
      <div style="margin-bottom: 1.5rem; padding-bottom: 1rem; border-bottom: 1px dashed var(--glass-border);">
        <p><strong>Q${idx + 1}: ${rev.questionText}</strong></p>
        
        <div style="margin: 10px 0; display:flex; flex-direction:column; gap:6px;">
          ${rev.options.map((opt, oIdx) => {
            let borderStyle = 'border: 1px solid var(--glass-border);';
            let bgStyle = 'background: rgba(0,0,0,0.1);';
            let checkmark = '';
            
            if (oIdx === rev.correctOptionIndex) {
              borderStyle = 'border-color: var(--success);';
              bgStyle = 'background: rgba(16, 185, 129, 0.1);';
              checkmark = ' <span style="color:var(--success);font-weight:700;">✓ (Correct Answer)</span>';
            } else if (oIdx === rev.studentAnswer && !isCorrect) {
              borderStyle = 'border-color: var(--error);';
              bgStyle = 'background: rgba(239, 68, 68, 0.1);';
              checkmark = ' <span style="color:var(--error);font-weight:700;">✗ (Your Answer)</span>';
            } else if (oIdx === rev.studentAnswer && isCorrect) {
              checkmark = ' <span style="color:var(--success);font-weight:700;">(Your Answer)</span>';
            }
            
            return `
              <div style="${borderStyle} ${bgStyle} padding: 8px 12px; border-radius: 8px; font-size:0.9rem; display:flex; justify-content:space-between;">
                <span>${String.fromCharCode(65 + oIdx)}: ${opt}</span>
                <span>${checkmark}</span>
              </div>
            `;
          }).join('')}
        </div>
        <p style="font-size:0.85rem; color: #a78bfa; margin-top:5px; line-height: 1.5;">
          <strong>AI Review Critique:</strong> ${rev.explanation}
        </p>
      </div>
    `;
  }).join('');
}

function toggleAccordion(id) {
  const content = document.getElementById(id);
  const header = content.previousElementSibling;
  const chevron = header.querySelector('.chevron');
  
  if (content.style.display === 'block') {
    content.style.display = 'none';
    chevron.style.transform = 'rotate(0deg)';
  } else {
    content.style.display = 'block';
    chevron.style.transform = 'rotate(180deg)';
  }
}

// 5. PROFILE CONTROL
async function loadStudentProfile() {
  const user = JSON.parse(localStorage.getItem(USER_KEY));
  if (user) {
    document.getElementById('profile-name').value = user.name || '';
    document.getElementById('profile-phone').value = user.phone || '';
    
    // Set class dropdown options
    const el = document.getElementById('profile-standard');
    el.innerHTML = '';
    Object.entries(CLASS_MAPPING).forEach(([code, label]) => {
      const opt = document.createElement('option');
      opt.value = code;
      opt.textContent = label;
      el.appendChild(opt);
    });
    el.value = user.standard || '';

    const avatar = document.getElementById('profile-avatar-img');
    if (user.avatarUrl && user.avatarUrl.trim() !== '') {
      avatar.src = user.avatarUrl;
    } else {
      avatar.src = 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200';
    }
  }
}

function triggerAvatarUpload() {
  document.getElementById('avatar-input').click();
}

async function uploadAvatar() {
  const fileInput = document.getElementById('avatar-input');
  if (fileInput.files.length === 0) return;
  
  const file = fileInput.files[0];
  const formData = new FormData();
  formData.append('file', file);
  formData.append('title', 'student_avatar_' + Date.now());
  formData.append('subject', 'Avatar');
  formData.append('classCode', 'System');
  
  showAlert('Uploading avatar...', false);
  
  try {
    // Student uses a fetch route to save image files
    const res = await fetchAPI('/api/admin/materials', { // We use same upload route for simplicity since it stores file
      method: 'POST',
      body: formData
    });
    
    if (res && res.ok) {
      const data = await res.json();
      const previewUrl = `/api/files/preview/${data.gridFsId}`;
      
      const user = JSON.parse(localStorage.getItem(USER_KEY));
      user.avatarUrl = previewUrl;
      localStorage.setItem(USER_KEY, JSON.stringify(user));
      
      document.getElementById('profile-avatar-img').src = previewUrl;
      document.querySelector('.user-profile-badge img').src = previewUrl;
      showAlert('Avatar uploaded successfully! Save settings below to save.', false);
    } else {
      showAlert('Failed to upload image.');
    }
  } catch (err) {
    showAlert('Error uploading avatar.');
  }
}

function setupProfileListener() {
  const form = document.getElementById('profile-form');
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('profile-name').value.trim();
    const phone = document.getElementById('profile-phone').value.trim();
    const standard = document.getElementById('profile-standard').value;
    const password = document.getElementById('profile-password').value;
    const avatarUrl = document.getElementById('profile-avatar-img').src;
    
    const body = { name, phone, standard, avatarUrl };
    if (password && password.trim() !== '') {
      body.password = password;
    }
    
    const res = await fetchAPI('/api/student/profile', {
      method: 'PUT',
      body: JSON.stringify(body)
    });
    
    if (res && res.ok) {
      const data = await res.json();
      const user = JSON.parse(localStorage.getItem(USER_KEY));
      user.name = data.name;
      user.phone = data.phone;
      user.standard = data.standard;
      user.avatarUrl = data.avatarUrl;
      localStorage.setItem(USER_KEY, JSON.stringify(user));
      
      // Update top header and personal banner
      document.querySelector('.user-profile-badge span').textContent = data.name;
      personalizeDashboard();
      
      showAlert('Settings updated successfully!', false);
      document.getElementById('profile-password').value = '';
    } else {
      showAlert('Failed to update settings details.');
    }
  });
}

// Modal Helpers
function openModal(id) {
  document.getElementById(id).classList.add('active');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('active');
  if (id === 'modal-preview') {
    document.getElementById('preview-frame').src = '';
  }
}
