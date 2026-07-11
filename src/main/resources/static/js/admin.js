// RM TUITION CENTER - Admin Dashboard Controller

const authInfo = checkAuth('ROLE_ADMIN');
if (!authInfo) {
  // If not authenticated admin, execution stops here (redirected by checkAuth)
}

// Subject options by class
const SUBJECTS_BY_CLASS = {
  '10': ['Tamil', 'English', 'Mathematics', 'Science', 'Social Science'],
  '11-all': ['Tamil', 'English', 'Mathematics', 'Physics', 'Chemistry', 'Computer Science'],
  '11-pcm': ['Physics', 'Chemistry', 'Mathematics'],
  '11-pc': ['Physics', 'Chemistry'],
  '11-maths': ['Mathematics'],
  '12-all': ['Tamil', 'English', 'Mathematics', 'Physics', 'Chemistry', 'Computer Science'],
  '12-pcm': ['Physics', 'Chemistry', 'Mathematics'],
  '12-pc': ['Physics', 'Chemistry'],
  '12-maths': ['Mathematics']
};

let generatedQuestions = []; // Hold temp generated quiz questions

document.addEventListener('DOMContentLoaded', () => {
  if (authInfo) {
    // 1. Initial Load
    loadStats();
    setupDropdowns();
    
    // Sidebar Tabs Router
    setupTabs();
    
    // Connect Forms
    setupFormListeners();
    
    // Default load sections
    loadMaterials();
    loadQuizzes();
    loadPapers();
    loadStudents();
    loadAdminProfile();
  }
});

// Load Dashboard Counts
async function loadStats() {
  const res = await fetchAPI('/api/admin/stats');
  if (res) {
    const data = await res.json();
    document.getElementById('stat-students').textContent = data.totalStudents;
    document.getElementById('stat-materials').textContent = data.totalMaterials;
    document.getElementById('stat-quizzes').textContent = data.totalQuizzes;
    document.getElementById('stat-papers').textContent = data.totalPapers;
  }
}

// Tab switcher router
function setupTabs() {
  const links = document.querySelectorAll('.sidebar-link[data-tab]');
  links.forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const tabName = link.getAttribute('data-tab');
      switchTab(tabName);
    });
  });
}

function switchTab(tabName) {
  // Update sidebar active class
  document.querySelectorAll('.sidebar-link').forEach(l => l.classList.remove('active'));
  const activeLink = document.querySelector(`.sidebar-link[data-tab="${tabName}"]`);
  if (activeLink) activeLink.classList.add('active');
  
  // Hide all sections, show active
  document.querySelectorAll('.tab-content').forEach(s => s.classList.add('hidden'));
  const targetSection = document.getElementById(`tab-${tabName}`);
  if (targetSection) targetSection.classList.remove('hidden');
  
  // Refresh stats if dashboard
  if (tabName === 'dashboard') {
    loadStats();
  }
}

// Dynamically populate class and subject dropdown options
function setupDropdowns() {
  const classDropdowns = [
    'as-standard', 'mat-class', 'paper-class', 'ai-class', 'mq-class'
  ];
  
  classDropdowns.forEach(id => {
    const el = document.getElementById(id);
    if (!el) return;
    
    el.innerHTML = '<option value="" disabled selected>Select Class</option>';
    Object.entries(CLASS_MAPPING).forEach(([code, label]) => {
      const opt = document.createElement('option');
      opt.value = code;
      opt.textContent = label;
      el.appendChild(opt);
    });
  });
  
  // Dynamic subjects matching dropdowns
  setupSubjectFilters('as-standard', null); // Student registration doesn't filter subjects
  setupSubjectFilters('mat-class', 'mat-subject');
  setupSubjectFilters('paper-class', 'paper-subject');
  setupSubjectFilters('ai-class', 'ai-subject');
  setupSubjectFilters('mq-class', 'mq-subject');
}

function setupSubjectFilters(classSelectId, subjectSelectId) {
  const classSelect = document.getElementById(classSelectId);
  const subjectSelect = document.getElementById(subjectSelectId);
  
  if (!classSelect || !subjectSelect) return;
  
  classSelect.addEventListener('change', () => {
    const classCode = classSelect.value;
    const subjects = SUBJECTS_BY_CLASS[classCode] || [];
    
    subjectSelect.innerHTML = '<option value="" disabled selected>Select Subject</option>';
    subjects.forEach(sub => {
      const opt = document.createElement('option');
      opt.value = sub;
      opt.textContent = sub;
      subjectSelect.appendChild(opt);
    });
  });
}

// Load Students
async function loadStudents() {
  const standardFilter = document.getElementById('filter-student-class').value;
  let url = '/api/admin/students';
  if (standardFilter) {
    url += `?standard=${standardFilter}`;
  }
  
  const res = await fetchAPI(url);
  if (res) {
    const students = await res.json();
    const tbody = document.querySelector('#students-table tbody');
    tbody.innerHTML = '';
    
    if (students.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center">No students found.</td></tr>`;
      return;
    }
    
    students.forEach(s => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${s.name}</td>
        <td>${s.email}</td>
        <td>${s.phone}</td>
        <td><span class="badge badge-violet">${CLASS_MAPPING[s.standard] || s.standard}</span></td>
        <td>${formatDate(s.createdAt)}</td>
        <td>
          <button class="btn btn-danger btn-sm" onclick="deleteStudent('${s.id}')">Delete</button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }
}

// Delete student
async function deleteStudent(id) {
  if (confirm('Are you sure you want to delete this student and all their exam logs?')) {
    const res = await fetchAPI(`/api/admin/students/${id}`, { method: 'DELETE' });
    if (res && res.ok) {
      loadStudents();
      loadStats();
    }
  }
}

// Load Materials
async function loadMaterials() {
  const filterClass = document.getElementById('filter-material-class').value;
  const filterSubject = document.getElementById('filter-material-subject').value;
  
  const res = await fetchAPI('/api/admin/materials');
  if (res) {
    let materials = await res.json();
    
    // Client-side filtering
    if (filterClass) {
      materials = materials.filter(m => m.classCode === filterClass);
    }
    if (filterSubject) {
      materials = materials.filter(m => m.subject === filterSubject);
    }
    
    const tbody = document.querySelector('#materials-table tbody');
    tbody.innerHTML = '';
    
    if (materials.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center">No study materials uploaded.</td></tr>`;
      return;
    }
    
    materials.forEach(m => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${m.title}</td>
        <td><span class="badge badge-violet">${CLASS_MAPPING[m.classCode] || m.classCode}</span></td>
        <td>${m.subject}</td>
        <td>${m.fileName}</td>
        <td>${formatDate(m.uploadedAt)}</td>
        <td>
          <div class="flex gap-2">
            <a href="/api/files/preview/${m.gridFsId}" target="_blank" class="btn btn-secondary btn-sm">Preview</a>
            <button class="btn btn-danger btn-sm" onclick="deleteMaterial('${m.id}')">Delete</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }
}

async function deleteMaterial(id) {
  if (confirm('Are you sure you want to delete this learning resource?')) {
    const res = await fetchAPI(`/api/admin/materials/${id}`, { method: 'DELETE' });
    if (res && res.ok) {
      loadMaterials();
      loadStats();
    }
  }
}

// Load Papers
async function loadPapers() {
  const res = await fetchAPI('/api/admin/papers');
  if (res) {
    const papers = await res.json();
    const tbody = document.querySelector('#papers-table tbody');
    tbody.innerHTML = '';
    
    if (papers.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center">No question papers uploaded.</td></tr>`;
      return;
    }
    
    papers.forEach(p => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${p.title}</td>
        <td><span class="badge badge-violet">${CLASS_MAPPING[p.classCode] || p.classCode}</span></td>
        <td>${p.subject}</td>
        <td>${p.fileName}</td>
        <td>${formatDate(p.uploadedAt)}</td>
        <td>
          <div class="flex gap-2">
            <a href="/api/files/preview/${p.gridFsId}" target="_blank" class="btn btn-secondary btn-sm">Preview</a>
            <button class="btn btn-danger btn-sm" onclick="deletePaper('${p.id}')">Delete</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }
}

async function deletePaper(id) {
  if (confirm('Are you sure you want to delete this question paper?')) {
    const res = await fetchAPI(`/api/admin/papers/${id}`, { method: 'DELETE' });
    if (res && res.ok) {
      loadPapers();
      loadStats();
    }
  }
}

// Load Quizzes
async function loadQuizzes() {
  const res = await fetchAPI('/api/admin/quizzes');
  if (res) {
    const quizzes = await res.json();
    const tbody = document.querySelector('#quizzes-table tbody');
    tbody.innerHTML = '';
    
    if (quizzes.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center">No quizzes created yet.</td></tr>`;
      return;
    }
    
    quizzes.forEach(q => {
      const tr = document.createElement('tr');
      
      let statusBadge = `<span class="badge badge-warning">Draft</span>`;
      let actionButtons = '';
      
      if (!q.published) {
        actionButtons += `<button class="btn btn-success btn-sm" onclick="publishQuiz('${q.id}')">Publish</button>`;
      } else {
        statusBadge = `<span class="badge badge-violet">Live / Published</span>`;
        if (!q.resultsReleased) {
          actionButtons += `<button class="btn btn-primary btn-sm" onclick="releaseResults('${q.id}')">Release Results</button>`;
        } else {
          statusBadge = `<span class="badge badge-success">Completed & Released</span>`;
        }
      }
      
      tr.innerHTML = `
        <td>${q.title}</td>
        <td><span class="badge badge-violet">${CLASS_MAPPING[q.classCode] || q.classCode}</span></td>
        <td>${q.subject}</td>
        <td>${q.questions ? q.questions.length : 0} Qs</td>
        <td style="font-size:0.85rem;">
          Start: ${formatDate(q.startWindow)}<br>
          End: ${formatDate(q.endWindow)}
        </td>
        <td>${statusBadge}</td>
        <td>
          <div class="flex gap-2 flex-wrap">
            ${actionButtons}
            <button class="btn btn-secondary btn-sm" onclick="viewLeaderboard('${q.id}', '${q.title}')">Leaderboard</button>
            <a href="/api/admin/quizzes/${q.id}/export" class="btn btn-secondary btn-sm" style="background:#0284c7;color:white;border:none;">Export Excel</a>
            <button class="btn btn-danger btn-sm" onclick="deleteQuiz('${q.id}')">Delete</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  }
}

async function publishQuiz(id) {
  if (confirm('Publish this quiz? Students will immediately be notified and can attempt it inside the window.')) {
    const res = await fetchAPI(`/api/admin/quizzes/${id}/publish`, { method: 'POST' });
    if (res && res.ok) {
      loadQuizzes();
    }
  }
}

async function releaseResults(id) {
  if (confirm('Release results? Students will immediately see their scores, correct answers, and AI critiques.')) {
    const res = await fetchAPI(`/api/admin/quizzes/${id}/release-results`, { method: 'POST' });
    if (res && res.ok) {
      loadQuizzes();
    }
  }
}

async function deleteQuiz(id) {
  if (confirm('Delete this quiz and all user score records?')) {
    const res = await fetchAPI(`/api/admin/quizzes/${id}`, { method: 'DELETE' });
    if (res && res.ok) {
      loadQuizzes();
      loadStats();
    }
  }
}

// View Leaderboard
async function viewLeaderboard(id, quizTitle) {
  const res = await fetchAPI(`/api/admin/quizzes/${id}/results`);
  if (res) {
    const attempts = await res.json();
    document.getElementById('leaderboard-title').textContent = `Scores & Leaderboard: ${quizTitle}`;
    
    // Sort attempts by score descending
    attempts.sort((a, b) => b.score - a.score);
    
    const tbody = document.querySelector('#leaderboard-table tbody');
    tbody.innerHTML = '';
    
    if (attempts.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="text-center">No student attempts recorded.</td></tr>`;
    } else {
      attempts.forEach((att, index) => {
        const percent = Math.round((att.score / att.totalQuestions) * 10000) / 100;
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td><strong>#${index + 1}</strong></td>
          <td>${att.studentName}</td>
          <td>${att.score} / ${att.totalQuestions}</td>
          <td>${percent}%</td>
          <td>${formatDate(att.submittedAt)}</td>
        `;
        tbody.appendChild(tr);
      });
    }
    
    openModal('modal-leaderboard');
  }
}

// 6. PROFILE CONFIGS
async function loadAdminProfile() {
  const user = JSON.parse(localStorage.getItem(USER_KEY));
  if (user) {
    document.getElementById('profile-name').value = user.name || '';
    document.getElementById('profile-phone').value = user.phone || '';
    
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
  formData.append('title', 'avatar_' + Date.now());
  formData.append('subject', 'Avatar');
  formData.append('classCode', 'System');
  
  showAlert('Uploading avatar...', false);
  
  try {
    // We upload avatar as standard material file, and link its preview URL to our profile
    const res = await fetchAPI('/api/admin/materials', {
      method: 'POST',
      body: formData
    });
    
    if (res && res.ok) {
      const data = await res.json();
      const previewUrl = `/api/files/preview/${data.gridFsId}`;
      
      // Update local storage and UI
      const user = JSON.parse(localStorage.getItem(USER_KEY));
      user.avatarUrl = previewUrl;
      localStorage.setItem(USER_KEY, JSON.stringify(user));
      
      document.getElementById('profile-avatar-img').src = previewUrl;
      document.querySelector('.user-profile-badge img').src = previewUrl;
      showAlert('Avatar uploaded successfully! Click save profile below to sync.', false);
    } else {
      showAlert('Failed to upload image.');
    }
  } catch (err) {
    showAlert('Error uploading avatar.');
  }
}

// 7. FORM LISTENERS
function setupFormListeners() {
  // Student registration manually
  const studentForm = document.getElementById('add-student-form');
  studentForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('as-name').value.trim();
    const email = document.getElementById('as-email').value.trim();
    const phone = document.getElementById('as-phone').value.trim();
    const standard = document.getElementById('as-standard').value;
    const password = document.getElementById('as-password').value;
    
    try {
      const res = await fetchAPI('/api/admin/students', {
        method: 'POST',
        body: JSON.stringify({ name, email, phone, standard, password })
      });
      
      if (res && res.ok) {
        closeModal('modal-add-student');
        loadStudents();
        loadStats();
        studentForm.reset();
      } else {
        const err = await res.json();
        showAlert(err.message || 'Failed to create student.', true, 'add-student-alert');
      }
    } catch (e) {
      showAlert('Connection error.', true, 'add-student-alert');
    }
  });
  
  // Upload Materials
  const matForm = document.getElementById('upload-material-form');
  matForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const classCode = document.getElementById('mat-class').value;
    const subject = document.getElementById('mat-subject').value;
    const title = document.getElementById('mat-title').value.trim();
    const file = document.getElementById('mat-file').files[0];
    
    const formData = new FormData();
    formData.append('classCode', classCode);
    formData.append('subject', subject);
    formData.append('title', title);
    formData.append('file', file);
    
    showAlert('Uploading material and mailing notifications...', false, 'upload-material-alert');
    
    const res = await fetchAPI('/api/admin/materials', {
      method: 'POST',
      body: formData
    });
    
    if (res && res.ok) {
      closeModal('modal-upload-material');
      loadMaterials();
      loadStats();
      matForm.reset();
    } else {
      showAlert('Failed to upload material.', true, 'upload-material-alert');
    }
  });

  // Upload Question Paper
  const paperForm = document.getElementById('upload-paper-form');
  paperForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const classCode = document.getElementById('paper-class').value;
    const subject = document.getElementById('paper-subject').value;
    const title = document.getElementById('paper-title').value.trim();
    const file = document.getElementById('paper-file').files[0];

    const formData = new FormData();
    formData.append('classCode', classCode);
    formData.append('subject', subject);
    formData.append('title', title);
    formData.append('file', file);

    showAlert('Uploading question paper and mailing notifications...', false, 'upload-paper-alert');

    const res = await fetchAPI('/api/admin/papers', {
      method: 'POST',
      body: formData
    });

    if (res && res.ok) {
      closeModal('modal-upload-paper');
      loadPapers();
      loadStats();
      paperForm.reset();
    } else {
      showAlert('Failed to upload practice paper.', true, 'upload-paper-alert');
    }
  });

  // Profile Save
  const profileForm = document.getElementById('profile-form');
  profileForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const name = document.getElementById('profile-name').value.trim();
    const phone = document.getElementById('profile-phone').value.trim();
    const password = document.getElementById('profile-password').value;
    const avatarUrl = document.getElementById('profile-avatar-img').src;
    
    const body = { name, phone, avatarUrl };
    if (password && password.trim() !== '') {
      body.password = password;
    }
    
    const res = await fetchAPI('/api/admin/profile', {
      method: 'PUT',
      body: JSON.stringify(body)
    });
    
    if (res && res.ok) {
      const data = await res.json();
      const user = JSON.parse(localStorage.getItem(USER_KEY));
      user.name = data.name;
      user.phone = data.phone;
      user.avatarUrl = data.avatarUrl;
      localStorage.setItem(USER_KEY, JSON.stringify(user));
      
      // Update top banner
      document.querySelector('.user-profile-badge span').textContent = data.name;
      
      showAlert('Profile saved successfully!', false);
      document.getElementById('profile-password').value = '';
    } else {
      showAlert('Failed to save profile details.');
    }
  });

  // AI Quiz Request
  document.getElementById('btn-generate-ai').addEventListener('click', async (e) => {
    e.preventDefault();
    const classCode = document.getElementById('ai-class').value;
    const subject = document.getElementById('ai-subject').value;
    const topic = document.getElementById('ai-topic').value.trim();
    const questionCount = document.getElementById('ai-count').value;
    
    if (!classCode || !subject || !topic || !questionCount) {
      showAlert('Please enter all generation inputs.', true, 'ai-quiz-alert');
      return;
    }
    
    showAlert('Invoking AI model to construct quiz questions... Please wait...', false, 'ai-quiz-alert');
    
    try {
      const res = await fetchAPI('/api/admin/quizzes/generate', {
        method: 'POST',
        body: JSON.stringify({ classCode, subject, topic, questionCount: parseInt(questionCount) })
      });
      
      if (res && res.ok) {
        generatedQuestions = await res.json();
        
        // Hide inputs, show preview panel
        document.getElementById('ai-quiz-inputs').classList.add('hidden');
        document.getElementById('ai-quiz-preview').classList.remove('hidden');
        document.getElementById('ai-quiz-alert').innerHTML = '';
        
        // Fill preview form defaults
        document.getElementById('ai-title').value = `${topic} Mock Quiz`;
        
        const listDiv = document.getElementById('ai-questions-list');
        listDiv.innerHTML = '';
        
        generatedQuestions.forEach((q, qIndex) => {
          const div = document.createElement('div');
          div.className = 'question-builder-item';
          div.innerHTML = `
            <p><strong>Q${qIndex + 1}: ${q.questionText}</strong></p>
            <ul style="margin: 10px 0 10px 20px; font-size:0.9rem;">
              ${q.options.map((opt, oIndex) => `
                <li style="${oIndex === q.correctOptionIndex ? 'color: var(--success); font-weight:600;' : ''}">
                  ${String.fromCharCode(65 + oIndex)}: ${opt}
                </li>
              `).join('')}
            </ul>
            <p style="font-size:0.85rem; color: var(--text-secondary);"><strong>Explanation:</strong> ${q.explanation}</p>
          `;
          listDiv.appendChild(div);
        });
      } else {
        showAlert('AI Gateway failed to produce questions.', true, 'ai-quiz-alert');
      }
    } catch (e) {
      showAlert('API error during AI call.', true, 'ai-quiz-alert');
    }
  });

  // Manual Quiz Setup
  const mqForm = document.getElementById('manual-quiz-form');
  mqForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const classCode = document.getElementById('mq-class').value;
    const subject = document.getElementById('mq-subject').value;
    const title = document.getElementById('mq-title').value.trim();
    const durationMinutes = parseInt(document.getElementById('mq-duration').value);
    const startWindow = new Date(document.getElementById('mq-start').value);
    const endWindow = new Date(document.getElementById('mq-end').value);
    
    // Parse manual questions
    const questions = [];
    const qDivs = document.querySelectorAll('.mq-q-item');
    
    if (qDivs.length === 0) {
      showAlert('Please add at least one question.', true, 'manual-quiz-alert');
      return;
    }
    
    for (let idx = 0; idx < qDivs.length; idx++) {
      const container = qDivs[idx];
      const text = container.querySelector('.mq-q-text').value.trim();
      const opA = container.querySelector('.mq-op-a').value.trim();
      const opB = container.querySelector('.mq-op-b').value.trim();
      const opC = container.querySelector('.mq-op-c').value.trim();
      const opD = container.querySelector('.mq-op-d').value.trim();
      const correct = parseInt(container.querySelector('.mq-q-correct').value);
      const expl = container.querySelector('.mq-q-expl').value.trim();
      
      questions.push({
        questionText: text,
        options: [opA, opB, opC, opD],
        correctOptionIndex: correct,
        explanation: expl
      });
    }
    
    const body = { classCode, subject, title, durationMinutes, startWindow, endWindow, questions };
    
    const res = await fetchAPI('/api/admin/quizzes', {
      method: 'POST',
      body: JSON.stringify(body)
    });
    
    if (res && res.ok) {
      closeModal('modal-manual-quiz');
      loadQuizzes();
      loadStats();
      mqForm.reset();
      document.getElementById('mq-questions-container').innerHTML = '';
    } else {
      showAlert('Failed to save manual quiz.', true, 'manual-quiz-alert');
    }
  });
}

// Reset AI Panel
function resetAIPanel() {
  document.getElementById('ai-quiz-inputs').classList.remove('hidden');
  document.getElementById('ai-quiz-preview').classList.add('hidden');
}

// Save AI Quiz
async function saveAIQuiz() {
  const classCode = document.getElementById('ai-class').value;
  const subject = document.getElementById('ai-subject').value;
  const title = document.getElementById('ai-title').value.trim();
  const durationMinutes = parseInt(document.getElementById('ai-duration').value);
  const startWindow = new Date(document.getElementById('ai-start').value);
  const endWindow = new Date(document.getElementById('ai-end').value);
  
  const body = {
    classCode,
    subject,
    title,
    durationMinutes,
    startWindow,
    endWindow,
    questions: generatedQuestions
  };
  
  const res = await fetchAPI('/api/admin/quizzes', {
    method: 'POST',
    body: JSON.stringify(body)
  });
  
  if (res && res.ok) {
    closeModal('modal-ai-quiz');
    loadQuizzes();
    loadStats();
    resetAIPanel();
  } else {
    showAlert('Failed to save generated quiz.', true, 'ai-quiz-alert');
  }
}

// Manual Questions Dynamic Chunks
let manualQCounter = 0;
function addManualQuestion() {
  manualQCounter++;
  const container = document.getElementById('mq-questions-container');
  const div = document.createElement('div');
  div.className = 'question-builder-item mq-q-item';
  div.dataset.id = manualQCounter;
  
  div.innerHTML = `
    <div class="flex justify-between align-center mb-4">
      <h5>Question #${manualQCounter}</h5>
      <button type="button" class="btn btn-danger btn-sm" onclick="this.closest('.mq-q-item').remove()">Remove</button>
    </div>
    
    <div class="form-group">
      <label>Question Prompt text</label>
      <input type="text" class="input-control mq-q-text" required>
    </div>
    
    <div style="display:grid; grid-template-columns: 1fr 1fr; gap:10px; margin-bottom:15px;">
      <div class="form-group" style="margin-bottom:0;">
        <label>Option A</label>
        <input type="text" class="input-control mq-op-a" required>
      </div>
      <div class="form-group" style="margin-bottom:0;">
        <label>Option B</label>
        <input type="text" class="input-control mq-op-b" required>
      </div>
      <div class="form-group" style="margin-bottom:0;">
        <label>Option C</label>
        <input type="text" class="input-control mq-op-c" required>
      </div>
      <div class="form-group" style="margin-bottom:0;">
        <label>Option D</label>
        <input type="text" class="input-control mq-op-d" required>
      </div>
    </div>
    
    <div class="form-group">
      <label>Correct Answer Index</label>
      <select class="input-control mq-q-correct" required>
        <option value="0">Option A</option>
        <option value="1">Option B</option>
        <option value="2">Option C</option>
        <option value="3">Option D</option>
      </select>
    </div>

    <div class="form-group">
      <label>Answer Explanation Critique</label>
      <input type="text" class="input-control mq-q-expl" required>
    </div>
  `;
  container.appendChild(div);
}

// Modal Helpers
function openModal(id) {
  document.getElementById(id).classList.add('active');
}
function closeModal(id) {
  document.getElementById(id).classList.remove('active');
  const alert = document.querySelector(`#${id} [id$="alert"]`);
  if (alert) alert.innerHTML = '';
}

// Shortcut triggers
function openAddStudentModal() {
  setupDropdowns();
  openModal('modal-add-student');
}
function openUploadMaterialModal() {
  setupDropdowns();
  openModal('modal-upload-material');
}
function openUploadPaperModal() {
  setupDropdowns();
  openModal('modal-upload-paper');
}
function openQuizGeneratorModal() {
  setupDropdowns();
  openModal('modal-ai-quiz');
}
function openManualQuizModal() {
  setupDropdowns();
  openModal('modal-manual-quiz');
}
