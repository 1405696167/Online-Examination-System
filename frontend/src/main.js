import { createApp, computed, onMounted, onUnmounted, reactive, ref } from 'vue/dist/vue.esm-bundler.js'
import './style.css'

const API = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

function notify(message, type = 'error') {
  if (window.examNotify) {
    window.examNotify(message, type)
  } else {
    console.warn(message)
  }
}

function askText(title, value = '') {
  if (window.examDialog) return window.examDialog({ type: 'text', title, value })
  return Promise.resolve(null)
}

function askConfirm(title) {
  if (window.examDialog) return window.examDialog({ type: 'confirm', title })
  return Promise.resolve(false)
}

async function request(path, options = {}) {
  const { silent, ...fetchOptions } = options
  const token = localStorage.getItem('examToken')
  try {
    const response = await fetch(`${API}${path}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(fetchOptions.headers || {}),
      },
      ...fetchOptions,
    })
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: '请求失败' }))
      const message = error.message || '请求失败'
      if (!silent) notify(message, response.status === 401 ? 'warning' : 'error')
      if (response.status === 401) {
        localStorage.removeItem('examToken')
        localStorage.removeItem('examUser')
        setTimeout(() => window.location.reload(), 300)
      }
      throw new Error(message)
    }
    return response.status === 204 ? null : response.json().catch(() => null)
  } catch (e) {
    if (!silent && e.name !== 'Error') {
      notify('网络连接失败，请确认后端服务已启动并已重启到最新代码', 'error')
    }
    throw e
  }
}

const App = {
  setup() {
    const user = ref(JSON.parse(localStorage.getItem('examUser') || 'null'))
    const role = ref('STUDENT')
    const loginForm = reactive({ username: '', password: '', role: 'STUDENT' })
    const error = ref('')
    const toasts = ref([])
    const dialog = ref(null)

    function pushToast(message, type = 'error') {
      const id = Date.now() + Math.random()
      toasts.value.push({ id, message, type })
      setTimeout(() => {
        toasts.value = toasts.value.filter(item => item.id !== id)
      }, 3600)
    }

    window.examNotify = pushToast

    function openDialog(config) {
      return new Promise(resolve => {
        dialog.value = {
          ...config,
          inputValue: config.value ?? '',
          resolve,
        }
      })
    }

    function confirmDialog() {
      if (!dialog.value) return
      const current = dialog.value
      dialog.value = null
      current.resolve(current.type === 'text' ? current.inputValue : true)
    }

    function cancelDialog() {
      if (!dialog.value) return
      const current = dialog.value
      dialog.value = null
      current.resolve(current.type === 'text' ? null : false)
    }

    window.examDialog = openDialog

    async function login() {
      error.value = ''
      try {
        user.value = await request('/auth/login', {
          method: 'POST',
          silent: true,
          body: JSON.stringify({ ...loginForm, role: role.value }),
        })
        localStorage.setItem('examToken', user.value.token)
        localStorage.setItem('examUser', JSON.stringify(user.value))
        pushToast('登录成功', 'success')
      } catch (e) {
        error.value = e.message
      }
    }

    async function logout() {
      if (window.examForceSubmit) {
        try {
          await window.examForceSubmit()
          pushToast('已强制交卷', 'success')
        } catch (e) {
          pushToast(e.message || '交卷失败，请重试', 'error')
          return
        }
      }
      user.value = null
      localStorage.removeItem('examToken')
      localStorage.removeItem('examUser')
      window.examForceSubmit = null
      pushToast('已退出登录', 'success')
    }

    return { user, role, loginForm, error, toasts, dialog, login, logout, confirmDialog, cancelDialog }
  },
  template: `
    <main>
      <div class="toast-stack">
        <div v-for="toast in toasts" :key="toast.id" class="toast" :class="toast.type">{{ toast.message }}</div>
      </div>
      <div v-if="dialog" class="dialog-backdrop">
        <section class="dialog-panel">
          <h2>{{ dialog.title }}</h2>
          <input v-if="dialog.type === 'text'" v-model="dialog.inputValue" @keyup.enter="confirmDialog" />
          <div class="form-actions">
            <button class="primary" @click="confirmDialog">确定</button>
            <button @click="cancelDialog">取消</button>
          </div>
        </section>
      </div>
      <section v-if="!user" class="login-shell">
        <div class="login-panel">
          <h1>在线考试系统</h1>
          <div class="segmented">
            <button v-for="item in ['STUDENT','TEACHER','ADMIN']" :class="{active: role===item}" @click="role=item">
              {{ item === 'STUDENT' ? '学生' : item === 'TEACHER' ? '教师' : '管理员' }}
            </button>
          </div>
          <label>{{ role === 'STUDENT' ? '学号' : '账号' }}<input v-model="loginForm.username" /></label>
          <label>密码<input v-model="loginForm.password" type="password" @keyup.enter="login" /></label>
          <button class="primary" @click="login">登录</button>
          <p class="error" v-if="error">{{ error }}</p>
          <p class="hint">管理员默认账号：admin / admin123</p>
        </div>
      </section>
      <section v-else>
        <header class="topbar">
          <div class="brand-lockup">
            <strong>在线考试系统</strong>
            <span>{{ user.role === 'ADMIN' ? '系统管理端' : user.role === 'TEACHER' ? '教师工作台' : '学生考试端' }}</span>
          </div>
          <div class="user-lockup">
            <strong>{{ user.name }}</strong>
            <span>{{ user.role === 'STUDENT' ? user.studentNumber : user.username }} · {{ user.className || '全校' }}</span>
          </div>
          <button @click="logout">退出</button>
        </header>
        <AdminDashboard v-if="user.role === 'ADMIN'" />
        <TeacherDashboard v-if="user.role === 'TEACHER'" :user="user" />
        <StudentDashboard v-if="user.role === 'STUDENT'" :user="user" />
      </section>
    </main>
  `,
}

App.components = {
  AdminDashboard: {
    setup() {
      const state = reactive({ teachers: [], teacherSubjects: [], students: [], subjects: [], classes: [], scores: [], msg: '' })
      const teacher = reactive({ username: '', password: '123456', name: '', className: '' })
      const student = reactive({ studentNumber: '', password: '123456', name: '', className: '' })
      const subjectName = ref('')
      const className = ref('')
      const activeTab = ref('overview')
      const assign = reactive({ teacherId: '', subjectIds: [] })
      const tabs = [
        { key: 'overview', label: '总览' },
        { key: 'teachers', label: '教师与科目' },
        { key: 'classes', label: '行政班级' },
        { key: 'students', label: '学生管理' },
        { key: 'scores', label: '成绩管理' },
      ]

      async function load() {
        state.teachers = await request('/admin/teachers')
        state.teacherSubjects = await request('/admin/teacher-subjects')
        state.students = await request('/admin/students')
        state.subjects = await request('/admin/subjects')
        state.classes = await request('/admin/classes')
        state.scores = await request('/admin/scores')
      }
      async function createTeacher() {
        await request('/admin/teachers', { method: 'POST', body: JSON.stringify(teacher) })
        Object.assign(teacher, { username: '', password: '123456', name: '', className: '' })
        await load()
      }
      async function editTeacher(t) {
        const name = await askText('教师姓名', t.name)
        if (name === null) return
        const username = await askText('教师账号', t.username)
        if (username === null) return
        const password = await askText('密码', '123456')
        if (password === null) return
        await request(`/admin/teachers/${t.id}`, { method: 'PUT', body: JSON.stringify({ name, username, className: '', password }) })
        await load()
      }
      async function deleteTeacher(id) {
        if (!(await askConfirm('确认删除该教师？'))) return
        await request(`/admin/teachers/${id}`, { method: 'DELETE' })
        await load()
      }
      async function createStudent() {
        if (!student.className) {
          notify('请先选择行政班级', 'warning')
          return
        }
        await request('/admin/students', { method: 'POST', body: JSON.stringify(student) })
        Object.assign(student, { studentNumber: '', password: '123456', name: '', className: '' })
        await load()
      }
      async function editStudent(s) {
        const name = await askText('学生姓名', s.name)
        if (name === null) return
        const studentNumber = await askText('学号', s.studentNumber)
        if (studentNumber === null) return
        const className = await askText('行政班级', s.className || '')
        if (className === null) return
        const password = await askText('密码', '123456')
        if (password === null) return
        await request(`/admin/students/${s.id}`, { method: 'PUT', body: JSON.stringify({ name, studentNumber, className, password }) })
        await load()
      }
      async function deleteStudent(id) {
        if (!(await askConfirm('确认删除该学生？'))) return
        await request(`/admin/students/${id}`, { method: 'DELETE' })
        await load()
      }
      async function createSubject() {
        if (!subjectName.value.trim()) return
        await request('/admin/subjects', { method: 'POST', body: JSON.stringify({ name: subjectName.value }) })
        subjectName.value = ''
        await load()
      }
      async function createClass() {
        if (!className.value.trim()) return
        await request('/admin/classes', { method: 'POST', body: JSON.stringify({ name: className.value }) })
        className.value = ''
        await load()
      }
      async function editClass(c) {
        const name = await askText('行政班级名称', c.name)
        if (!name) return
        await request(`/admin/classes/${c.id}`, { method: 'PUT', body: JSON.stringify({ name }) })
        await load()
      }
      async function deleteClass(id) {
        if (!(await askConfirm('确认删除该行政班级？已有学生的班级不能删除。'))) return
        await request(`/admin/classes/${id}`, { method: 'DELETE' })
        await load()
      }
      async function editSubject(s) {
        const name = await askText('科目名称', s.name)
        if (!name) return
        await request(`/admin/subjects/${s.id}`, { method: 'PUT', body: JSON.stringify({ name }) })
        await load()
      }
      async function deleteSubject(id) {
        if (!(await askConfirm('确认删除该科目？相关课程班关系会同步移除。'))) return
        await request(`/admin/subjects/${id}`, { method: 'DELETE' })
        await load()
      }
      async function assignSubjects() {
        await request('/admin/teacher-subjects', { method: 'POST', body: JSON.stringify(assign) })
        state.msg = '科目分配完成'
        await load()
      }
      async function updateScore(score) {
        const input = await askText('输入新分数', score.score)
        if (input === null) return
        const value = Number(input)
        if (Number.isFinite(value)) {
          await request(`/admin/scores/${score.id}`, { method: 'PUT', body: JSON.stringify({ score: value }) })
          await load()
        }
      }
      onMounted(load)
      return { state, teacher, student, subjectName, className, activeTab, tabs, assign, createTeacher, editTeacher, deleteTeacher, createStudent, editStudent, deleteStudent, createSubject, editSubject, deleteSubject, createClass, editClass, deleteClass, assignSubjects, updateScore }
    },
    template: `
      <div class="workspace">
        <nav class="tabs">
          <button v-for="tab in tabs" :class="{active: activeTab === tab.key}" @click="activeTab = tab.key">{{ tab.label }}</button>
        </nav>

        <div v-if="activeTab === 'overview'" class="grid">
          <section class="metric"><span>教师</span><strong>{{ state.teachers.length }}</strong></section>
          <section class="metric"><span>学生</span><strong>{{ state.students.length }}</strong></section>
          <section class="metric"><span>行政班</span><strong>{{ state.classes.length }}</strong></section>
          <section class="metric"><span>科目</span><strong>{{ state.subjects.length }}</strong></section>
          <section class="metric"><span>成绩记录</span><strong>{{ state.scores.length }}</strong></section>
          <section class="panel wide">
            <h2>教师授课概览</h2>
            <table>
              <thead><tr><th>教师</th><th>账号</th><th>教授科目</th></tr></thead>
              <tbody>
                <tr v-for="t in state.teacherSubjects">
                  <td>{{ t.teacherName }}</td>
                  <td>{{ t.username }}</td>
                  <td>{{ t.subjects.length ? t.subjects.join('、') : '未分配' }}</td>
                </tr>
              </tbody>
            </table>
          </section>
        </div>

        <div v-if="activeTab === 'teachers'" class="grid">
          <section class="panel">
            <h2>创建教师</h2>
            <input v-model="teacher.username" placeholder="教师账号" />
            <input v-model="teacher.name" placeholder="姓名" />
            <input v-model="teacher.password" placeholder="密码" />
            <button class="primary" @click="createTeacher">创建</button>
          </section>
          <section class="panel">
            <h2>科目分配</h2>
            <div class="row"><input v-model="subjectName" placeholder="科目名称" /><button @click="createSubject">新增</button></div>
            <select v-model="assign.teacherId"><option disabled value="">选择教师</option><option v-for="t in state.teachers" :value="t.id">{{t.name}} · {{t.username}}</option></select>
            <div class="checks"><label v-for="s in state.subjects"><input type="checkbox" :value="s.id" v-model="assign.subjectIds" />{{s.name}}</label></div>
            <button class="primary" @click="assignSubjects">保存分配</button>
            <p class="hint">{{ state.msg }}</p>
          </section>
          <section class="panel wide">
            <h2>教师与科目</h2>
            <table>
              <thead><tr><th>教师</th><th>账号</th><th>教授科目</th><th></th></tr></thead>
              <tbody>
                <tr v-for="t in state.teacherSubjects">
                  <td>{{ t.teacherName }}</td>
                  <td>{{ t.username }}</td>
                  <td>{{ t.subjects.length ? t.subjects.join('、') : '未分配' }}</td>
                  <td><div class="actions"><button @click="editTeacher(state.teachers.find(x => x.id === t.teacherId))">编辑</button><button @click="deleteTeacher(t.teacherId)">删除</button></div></td>
                </tr>
              </tbody>
            </table>
          </section>
          <section class="panel wide">
            <h2>科目列表</h2>
            <table><thead><tr><th>科目</th><th></th></tr></thead><tbody><tr v-for="s in state.subjects"><td>{{s.name}}</td><td><div class="actions"><button @click="editSubject(s)">编辑</button><button @click="deleteSubject(s.id)">删除</button></div></td></tr></tbody></table>
          </section>
        </div>

        <div v-if="activeTab === 'classes'" class="grid">
          <section class="panel">
            <h2>新增行政班级</h2>
            <input v-model="className" placeholder="如 计算机1班" />
            <button class="primary" @click="createClass">新增</button>
          </section>
          <section class="panel wide">
            <h2>行政班级列表</h2>
            <table><thead><tr><th>班级名称</th><th></th></tr></thead><tbody><tr v-for="c in state.classes"><td>{{c.name}}</td><td><div class="actions"><button @click="editClass(c)">编辑</button><button @click="deleteClass(c.id)">删除</button></div></td></tr></tbody></table>
          </section>
        </div>

        <div v-if="activeTab === 'students'" class="grid">
          <section class="panel">
            <h2>创建学生</h2>
            <input v-model="student.studentNumber" placeholder="学号" />
            <input v-model="student.name" placeholder="姓名" />
            <select v-model="student.className"><option disabled value="">选择行政班级</option><option v-for="c in state.classes" :value="c.name">{{c.name}}</option></select>
            <input v-model="student.password" placeholder="密码" />
            <button class="primary" @click="createStudent">添加</button>
          </section>
          <section class="panel wide">
            <h2>学生列表</h2>
            <table>
              <thead><tr><th>姓名</th><th>学号</th><th>行政班级</th><th></th></tr></thead>
              <tbody>
                <tr v-for="s in state.students">
                  <td>{{ s.name }}</td>
                  <td>{{ s.studentNumber }}</td>
                  <td>{{ s.className }}</td>
                  <td><div class="actions"><button @click="editStudent(s)">编辑</button><button @click="deleteStudent(s.id)">删除</button></div></td>
                </tr>
              </tbody>
            </table>
          </section>
        </div>

        <div v-if="activeTab === 'scores'" class="grid">
          <section class="panel wide">
            <h2>成绩管理</h2>
            <table><thead><tr><th>学生</th><th>学号</th><th>考试</th><th>科目</th><th>分数</th><th>阅卷</th><th>切屏</th><th></th></tr></thead>
            <tbody><tr v-for="s in state.scores"><td>{{s.studentName}}</td><td>{{s.studentNumber}}</td><td>{{s.examTitle}}</td><td>{{s.subjectName}}</td><td>{{s.score}}</td><td>{{s.graded ? '已完成' : '待阅卷'}}</td><td>{{s.switchCount}}</td><td><button @click="updateScore(s)">改分</button></td></tr></tbody></table>
          </section>
        </div>
      </div>
    `,
  },
  TeacherDashboard: {
    props: ['user'],
    setup(props) {
      const state = reactive({ subjects: [], courseClasses: [], adminClasses: [], questions: [], exams: [], scores: [], students: [], reviews: [] })
      const question = reactive({ subjectId: '', questionType: 'SINGLE_CHOICE', content: '', optionA: '', optionB: '', optionC: '', optionD: '', answer: 'A', score: 5 })
      const exam = reactive({ courseClassId: '', title: '', questionCount: 5, durationMinutes: 45, active: true, deadline: '' })
      const courseClass = reactive({ subjectId: '', name: '' })
      const batchImport = reactive({ courseClassId: '', adminClassName: '' })
      const singleStudent = reactive({ courseClassId: '', name: '', studentNumber: '' })
      const activeTab = ref('questions')
      const activeReview = ref(null)
      const reviewScores = reactive({})
      const tabs = [
        { key: 'questions', label: '题库' },
        { key: 'exams', label: '考试' },
        { key: 'students', label: '课程学生' },
        { key: 'reviews', label: '阅卷' },
        { key: 'scores', label: '成绩' },
      ]
      async function load() {
        state.subjects = await request(`/teacher/${props.user.id}/subjects`)
        state.courseClasses = await request(`/teacher/${props.user.id}/course-classes`)
        state.adminClasses = await request('/admin/classes')
        state.questions = await request(`/teacher/${props.user.id}/questions`)
        state.exams = await request(`/teacher/${props.user.id}/exams`)
        state.scores = await request(`/teacher/${props.user.id}/scores`)
        state.students = await request(`/teacher/${props.user.id}/students`)
        state.reviews = await request(`/teacher/${props.user.id}/reviews`)
      }
      async function saveQuestion() {
        await request('/teacher/questions', { method: 'POST', body: JSON.stringify({ ...question, teacherId: props.user.id, subjectId: Number(question.subjectId), score: Number(question.score) }) })
        Object.assign(question, { subjectId: '', questionType: 'SINGLE_CHOICE', content: '', optionA: '', optionB: '', optionC: '', optionD: '', answer: 'A', score: 5 })
        await load()
      }
      async function deleteQuestion(id) {
        await request(`/teacher/questions/${id}`, { method: 'DELETE' })
        await load()
      }
      async function editQuestion(q) {
        const content = await askText('题目内容', q.content)
        if (content === null) return
        const optionA = await askText('A 选项', q.optionA || '')
        if (optionA === null) return
        const optionB = await askText('B 选项', q.optionB || '')
        if (optionB === null) return
        const optionC = await askText('C 选项', q.optionC || '')
        if (optionC === null) return
        const optionD = await askText('D 选项', q.optionD || '')
        if (optionD === null) return
        const answer = await askText('正确答案 A/B/C/D', q.answer || '')
        if (answer === null) return
        const scoreInput = await askText('分值', q.score)
        if (scoreInput === null) return
        const score = Number(scoreInput)
        if (!Number.isFinite(score)) return
        await request(`/teacher/questions/${q.id}`, {
          method: 'PUT',
          body: JSON.stringify({ teacherId: props.user.id, subjectId: q.subject.id, questionType: q.questionType || 'SINGLE_CHOICE', content, optionA, optionB, optionC, optionD, answer, score }),
        })
        await load()
      }
      async function createExam() {
        const selected = state.courseClasses.find(c => c.id === Number(exam.courseClassId))
        if (!selected) {
          notify('请先选择课程班级', 'warning')
          return
        }
        await request('/teacher/exams', { method: 'POST', body: JSON.stringify({ teacherId: props.user.id, subjectId: selected.subjectId, title: exam.title, className: selected.name, questionCount: Number(exam.questionCount), durationMinutes: Number(exam.durationMinutes), active: exam.active, deadline: exam.deadline }) })
        await load()
      }
      async function editExam(e) {
        const title = await askText('考试名称', e.title)
        if (title === null) return
        const className = await askText('课程班级', e.className)
        if (className === null) return
        const questionCountInput = await askText('题数', e.questionCount)
        if (questionCountInput === null) return
        const questionCount = Number(questionCountInput)
        if (!Number.isFinite(questionCount)) return
        const durationInput = await askText('时长分钟', e.durationMinutes)
        if (durationInput === null) return
        const durationMinutes = Number(durationInput)
        if (!Number.isFinite(durationMinutes)) return
        const active = await askConfirm('是否开放考试？确定=开放，取消=关闭')
        const deadline = await askText('截止时间，格式 2026-06-01T18:00，可留空', e.deadline || '')
        if (deadline === null) return
        await request(`/teacher/exams/${e.id}`, { method: 'PUT', body: JSON.stringify({ teacherId: props.user.id, subjectId: e.subjectId, title, className, questionCount, durationMinutes, active, deadline }) })
        await load()
      }
      async function deleteExam(id) {
        if (!(await askConfirm('确认删除该考试？'))) return
        await request(`/teacher/exams/${id}`, { method: 'DELETE' })
        await load()
      }
      async function createCourseClass() {
        if (!courseClass.subjectId || !courseClass.name.trim()) {
          notify('请选择课程并填写课程班级名称', 'warning')
          return
        }
        await request('/teacher/course-classes', { method: 'POST', body: JSON.stringify({ teacherId: props.user.id, subjectId: Number(courseClass.subjectId), name: courseClass.name }) })
        Object.assign(courseClass, { subjectId: '', name: '' })
        await load()
      }
      async function deleteCourseClass(id) {
        if (!(await askConfirm('确认删除该课程班？课程班学生关系会同步移除。'))) return
        await request(`/teacher/course-classes/${id}`, { method: 'DELETE' })
        await load()
      }
      async function importAdminClass() {
        if (!batchImport.courseClassId || !batchImport.adminClassName) {
          notify('请选择课程班级和行政班级', 'warning')
          return
        }
        await request('/teacher/students/import-class', { method: 'POST', body: JSON.stringify({ teacherId: props.user.id, courseClassId: Number(batchImport.courseClassId), adminClassName: batchImport.adminClassName }) })
        await load()
      }
      async function addSingleStudent() {
        if (!singleStudent.courseClassId || !singleStudent.name.trim() || !singleStudent.studentNumber.trim()) {
          notify('请选择课程班级，并填写学生姓名和学号', 'warning')
          return
        }
        await request('/teacher/students/single', {
          method: 'POST',
          body: JSON.stringify({ teacherId: props.user.id, courseClassId: Number(singleStudent.courseClassId), name: singleStudent.name, studentNumber: singleStudent.studentNumber }),
        })
        Object.assign(singleStudent, { courseClassId: '', name: '', studentNumber: '' })
        await load()
      }
      async function removeEnrollment(id) {
        if (!(await askConfirm('确认将该学生移出课程班？'))) return
        await request(`/teacher/students/${id}`, { method: 'DELETE' })
        await load()
      }
      async function updateScore(s) {
        const input = await askText('输入新分数', s.score)
        if (input === null) return
        const value = Number(input)
        if (Number.isFinite(value)) {
          await request(`/teacher/scores/${s.id}`, { method: 'PUT', body: JSON.stringify({ score: value }) })
          await load()
        }
      }
      function openReview(review) {
        activeReview.value = review
        Object.keys(reviewScores).forEach(key => delete reviewScores[key])
        review.questions
          .filter(item => item.questionType === 'ESSAY')
          .forEach(item => {
            reviewScores[item.id] = item.manualScore ?? 0
          })
      }
      function closeReview() {
        activeReview.value = null
        Object.keys(reviewScores).forEach(key => delete reviewScores[key])
      }
      async function submitReview() {
        if (!activeReview.value) return
        const manualScores = {}
        for (const q of activeReview.value.questions.filter(item => item.questionType === 'ESSAY')) {
          const value = Number(reviewScores[q.id])
          if (!Number.isFinite(value) || value < 0 || value > q.maxScore) {
            notify(`“${q.content}”的分数需在 0 到 ${q.maxScore} 之间`, 'warning')
            return
          }
          manualScores[q.id] = value
        }
        await request(`/teacher/reviews/${activeReview.value.studentExamId}`, { method: 'POST', body: JSON.stringify({ manualScores }) })
        notify('阅卷完成，最终分数已更新', 'success')
        closeReview()
        await load()
      }
      onMounted(load)
      return { state, question, exam, courseClass, batchImport, singleStudent, activeTab, activeReview, reviewScores, tabs, saveQuestion, editQuestion, deleteQuestion, createExam, editExam, deleteExam, createCourseClass, deleteCourseClass, importAdminClass, addSingleStudent, removeEnrollment, updateScore, openReview, closeReview, submitReview }
    },
    template: `
      <div class="workspace">
        <nav class="tabs">
          <button v-for="tab in tabs" :class="{active: activeTab === tab.key}" @click="activeTab = tab.key">{{ tab.label }}</button>
        </nav>

        <div v-if="activeTab === 'questions'" class="grid">
          <section class="panel">
            <h2>新增题目</h2>
            <select v-model="question.subjectId"><option disabled value="">科目</option><option v-for="s in state.subjects" :value="s.id">{{s.name}}</option></select>
            <select v-model="question.questionType"><option value="SINGLE_CHOICE">单选题</option><option value="BLANK">填空题</option><option value="ESSAY">简答题</option></select>
            <textarea v-model="question.content" placeholder="题目"></textarea>
            <template v-if="question.questionType === 'SINGLE_CHOICE'">
              <input v-model="question.optionA" placeholder="A 选项" /><input v-model="question.optionB" placeholder="B 选项" />
              <input v-model="question.optionC" placeholder="C 选项" /><input v-model="question.optionD" placeholder="D 选项" />
              <select v-model="question.answer"><option>A</option><option>B</option><option>C</option><option>D</option></select>
            </template>
            <input v-if="question.questionType === 'BLANK'" v-model="question.answer" placeholder="填空题标准答案" />
            <textarea v-if="question.questionType === 'ESSAY'" v-model="question.answer" placeholder="简答题参考答案，不自动判分"></textarea>
            <input v-model="question.score" type="number" placeholder="分值" />
            <button class="primary" @click="saveQuestion">保存题目</button>
          </section>
          <section class="panel wide">
            <h2>题库列表</h2>
            <table><thead><tr><th>科目</th><th>题型</th><th>题目</th><th>答案/参考</th><th>分值</th><th></th></tr></thead><tbody><tr v-for="q in state.questions"><td>{{q.subject.name}}</td><td>{{!q.questionType || q.questionType === 'SINGLE_CHOICE' ? '单选' : q.questionType === 'BLANK' ? '填空' : '简答'}}</td><td>{{q.content}}</td><td>{{q.answer}}</td><td>{{q.score}}</td><td><div class="actions"><button @click="editQuestion(q)">编辑</button><button @click="deleteQuestion(q.id)">删除</button></div></td></tr></tbody></table>
          </section>
        </div>

        <div v-if="activeTab === 'exams'" class="grid">
          <section class="panel">
            <h2>发布考试</h2>
            <input v-model="exam.title" placeholder="考试名称" />
            <select v-model="exam.courseClassId"><option disabled value="">选择课程班级</option><option v-for="c in state.courseClasses" :value="c.id">{{c.name}} · {{c.subjectName}}</option></select>
            <div class="row"><input v-model="exam.questionCount" type="number" placeholder="题数" /><input v-model="exam.durationMinutes" type="number" placeholder="分钟" /></div>
            <label>截止时间<input v-model="exam.deadline" type="datetime-local" /></label>
            <label class="inline"><input type="checkbox" v-model="exam.active" />开放考试</label>
            <button class="primary" @click="createExam">发布</button>
          </section>
          <section class="panel wide">
            <h2>考试列表</h2>
            <table><thead><tr><th>考试</th><th>科目</th><th>课程班级</th><th>题数</th><th>时长</th><th>截止时间</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="e in state.exams"><td>{{e.title}}</td><td>{{e.subjectName}}</td><td>{{e.className}}</td><td>{{e.questionCount}}</td><td>{{e.durationMinutes}} 分钟</td><td>{{e.deadline || '不限'}}</td><td>{{e.active ? '开放' : '关闭'}}</td><td><div class="actions"><button @click="editExam(e)">编辑</button><button @click="deleteExam(e.id)">删除</button></div></td></tr></tbody></table>
          </section>
        </div>

        <div v-if="activeTab === 'students'" class="grid">
          <section class="panel">
            <h2>创建课程班</h2>
            <select v-model="courseClass.subjectId"><option disabled value="">选择课程</option><option v-for="s in state.subjects" :value="s.id">{{s.name}}</option></select>
            <input v-model="courseClass.name" placeholder="课程班级名称，如 Java1班" />
            <button class="primary" @click="createCourseClass">创建课程班</button>
          </section>
          <section class="panel">
            <h2>批量加入学生</h2>
            <select v-model="batchImport.courseClassId"><option disabled value="">选择课程班级</option><option v-for="c in state.courseClasses" :value="c.id">{{c.name}} · {{c.subjectName}}</option></select>
            <select v-model="batchImport.adminClassName"><option disabled value="">选择行政班级</option><option v-for="c in state.adminClasses" :value="c.name">{{c.name}}</option></select>
            <button class="primary" @click="importAdminClass">将行政班学生加入课程班</button>
            <p class="hint">批量导入不会修改学生行政班级。</p>
          </section>
          <section class="panel">
            <h2>单个加入学生</h2>
            <select v-model="singleStudent.courseClassId"><option disabled value="">选择课程班级</option><option v-for="c in state.courseClasses" :value="c.id">{{c.name}} · {{c.subjectName}}</option></select>
            <input v-model="singleStudent.name" placeholder="学生姓名" />
            <input v-model="singleStudent.studentNumber" placeholder="学号" />
            <button class="primary" @click="addSingleStudent">加入课程班</button>
            <p class="hint">无需输入密码；已有学生会保留原密码。</p>
          </section>
          <section class="panel wide">
            <h2>课程班列表</h2>
            <table><thead><tr><th>课程班级</th><th>课程</th><th></th></tr></thead><tbody><tr v-for="c in state.courseClasses"><td>{{c.name}}</td><td>{{c.subjectName}}</td><td><button @click="deleteCourseClass(c.id)">删除</button></td></tr></tbody></table>
          </section>
          <section class="panel wide">
            <h2>课程班学生</h2>
            <table><thead><tr><th>姓名</th><th>学号</th><th>行政班级</th><th>课程</th><th>课程班级</th><th></th></tr></thead><tbody><tr v-for="s in state.students"><td>{{s.studentName}}</td><td>{{s.studentNumber}}</td><td>{{s.adminClassName}}</td><td>{{s.subjectName}}</td><td>{{s.courseClassName}}</td><td><button @click="removeEnrollment(s.id)">移出</button></td></tr></tbody></table>
          </section>
        </div>

        <div v-if="activeTab === 'scores'" class="grid">
          <section class="panel wide">
            <h2>成绩列表</h2>
            <table><thead><tr><th>学生</th><th>考试</th><th>科目</th><th>分数</th><th>阅卷</th><th>切屏</th><th></th></tr></thead><tbody><tr v-for="s in state.scores"><td>{{s.studentName}}</td><td>{{s.examTitle}}</td><td>{{s.subjectName}}</td><td>{{s.score}}</td><td>{{s.graded ? '已完成' : '待阅卷'}}</td><td>{{s.switchCount}}</td><td><button @click="updateScore(s)">改分</button></td></tr></tbody></table>
          </section>
        </div>

        <div v-if="activeTab === 'reviews'" class="grid">
          <section class="panel wide">
            <h2>主观题阅卷</h2>
            <table><thead><tr><th>学生</th><th>学号</th><th>考试</th><th>科目</th><th>当前分</th><th>状态</th><th></th></tr></thead><tbody><tr v-for="r in state.reviews"><td>{{r.studentName}}</td><td>{{r.studentNumber}}</td><td>{{r.examTitle}}</td><td>{{r.subjectName}}</td><td>{{r.currentScore}}</td><td>{{r.graded ? '已阅卷' : '待阅卷'}}</td><td><button class="primary" @click="openReview(r)">阅卷</button></td></tr></tbody></table>
          </section>
          <section v-if="activeReview" class="panel wide review-board">
            <div class="review-head">
              <div>
                <h2>{{ activeReview.studentName }} · {{ activeReview.examTitle }}</h2>
                <p class="hint">{{ activeReview.studentNumber }} · {{ activeReview.subjectName }} · 当前自动得分 {{ activeReview.currentScore }}</p>
              </div>
              <button @click="closeReview">关闭</button>
            </div>
            <article v-for="q in activeReview.questions.filter(item => item.questionType === 'ESSAY')" class="review-item">
              <div class="review-question">
                <strong>{{ q.content }}</strong>
                <span>满分 {{ q.maxScore }} 分</span>
              </div>
              <div class="answer-grid">
                <div class="answer-block">
                  <span>学生答案</span>
                  <p>{{ q.studentAnswer || '未作答' }}</p>
                </div>
                <div class="answer-block">
                  <span>参考答案</span>
                  <p>{{ q.referenceAnswer || '无' }}</p>
                </div>
              </div>
              <label class="score-input">本题得分<input v-model.number="reviewScores[q.id]" type="number" min="0" :max="q.maxScore" /></label>
            </article>
            <div class="form-actions">
              <button class="primary" @click="submitReview">提交阅卷</button>
              <button @click="closeReview">取消</button>
            </div>
          </section>
        </div>
      </div>
    `,
  },
  StudentDashboard: {
    props: ['user'],
    setup(props) {
      const exams = ref([])
      const paper = ref(null)
      const answers = reactive({})
      const result = ref(null)
      const warning = ref('')
      const lastSwitchAt = ref(0)
      const submitting = ref(false)
      const examMode = computed(() => paper.value && !result.value)
      async function load() {
        exams.value = await request(`/student/${props.user.id}/exams`)
      }
      async function start(examId) {
        paper.value = await request(`/student/${props.user.id}/exams/${examId}/start`, { method: 'POST' })
        result.value = null
        warning.value = ''
      }
      async function submit() {
        if (!paper.value || submitting.value) return result.value
        submitting.value = true
        try {
          result.value = await request('/student/submit', { method: 'POST', body: JSON.stringify({ studentExamId: paper.value.studentExamId, answers }) })
          paper.value = null
          Object.keys(answers).forEach(key => delete answers[key])
          await load()
          return result.value
        } finally {
          submitting.value = false
        }
      }
      async function forceSubmitBeforeLogout() {
        if (!paper.value) return null
        warning.value = '正在强制交卷，请稍候'
        return submit()
      }
      async function report(eventType) {
        if (!paper.value) return
        const now = Date.now()
        if (now - lastSwitchAt.value < 1800) return
        lastSwitchAt.value = now
        warning.value = '系统已记录切屏行为'
        await request('/student/switch-events', { method: 'POST', silent: true, body: JSON.stringify({ studentExamId: paper.value.studentExamId, eventType, visibilityState: document.visibilityState }) }).catch(() => {})
      }
      const visibilityHandler = () => {
        if (document.visibilityState !== 'visible') report('visibilitychange')
      }
      const blurHandler = () => report('blur')
      onMounted(() => {
        load()
        window.examForceSubmit = forceSubmitBeforeLogout
        document.addEventListener('visibilitychange', visibilityHandler)
        window.addEventListener('blur', blurHandler)
      })
      onUnmounted(() => {
        if (window.examForceSubmit === forceSubmitBeforeLogout) window.examForceSubmit = null
        document.removeEventListener('visibilitychange', visibilityHandler)
        window.removeEventListener('blur', blurHandler)
      })
      return { exams, paper, answers, result, warning, examMode, submitting, start, submit }
    },
    template: `
      <div class="student">
        <section v-if="!examMode" class="panel wide">
          <h2>可参加考试</h2>
          <div class="exam-list">
            <article v-for="e in exams" class="exam-card">
              <strong>{{ e.subjectName }}</strong><span>{{ e.title }} · {{ e.durationMinutes }} 分钟 · {{ e.questionCount }} 题 · 截止 {{ e.deadline || '不限' }}</span>
              <button class="primary" @click="start(e.id)">开始考试</button>
            </article>
          </div>
          <p v-if="result" class="success">提交成功，{{ result.graded ? '得分 ' + result.score : '主观题待老师阅卷，当前自动得分 ' + result.score }}，切屏 {{ result.switchCount }} 次。</p>
        </section>
        <section v-else class="panel exam-paper">
          <h2>{{ paper.exam.title }}</h2>
          <p class="error" v-if="warning">{{ warning }}</p>
          <article v-for="(q, index) in paper.questions" class="question">
            <strong>{{ index + 1 }}. {{ q.content }}</strong>
            <template v-if="!q.questionType || q.questionType === 'SINGLE_CHOICE'">
              <label v-for="key in ['A','B','C','D']"><input type="radio" :name="'q'+q.id" :value="key" v-model="answers[q.id]" />{{ key }}. {{ q['option' + key] }}</label>
            </template>
            <input v-if="q.questionType === 'BLANK'" v-model="answers[q.id]" placeholder="填写答案" />
            <textarea v-if="q.questionType === 'ESSAY'" v-model="answers[q.id]" placeholder="请输入简答题答案"></textarea>
          </article>
          <button class="primary" :disabled="submitting" @click="submit">{{ submitting ? '交卷中' : '交卷' }}</button>
        </section>
      </div>
    `,
  },
}

createApp(App).mount('#app')
