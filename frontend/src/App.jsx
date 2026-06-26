import { useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import { Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import './App.css'

const API_BASE = import.meta.env.VITE_API_URL || '/api'
const SESSION_KEY = 'eventflow.session'
const api = axios.create({ baseURL: API_BASE })

const demoAccounts = [
  { role: 'Admin', email: 'admin@eventflow.dev', senha: 'Admin@1234' },
  { role: 'Professor', email: 'professor@eventflow.dev', senha: 'Prof@1234' },
  { role: 'Aluno', email: 'aluno@eventflow.dev', senha: 'Aluno@1234' },
]

const emptyLogin = { email: '', senha: '' }
const emptyRegister = { nome: '', email: '', senha: '' }
const emptyEvent = {
  titulo: '',
  descricao: '',
  dataInicio: '',
  dataFim: '',
  local: '',
  cargaHoraria: '',
}
const emptyTurma = {
  eventoId: '',
  nome: '',
  dataHoraInicio: '',
  dataHoraFim: '',
  vagas: '',
  professorId: '',
}

function readStoredSession() {
  try {
    const stored = localStorage.getItem(SESSION_KEY)
    if (!stored) return null
    const parsed = JSON.parse(stored)
    if (!parsed?.accessToken) return null
    return { ...parsed, user: decodeToken(parsed.accessToken) }
  } catch {
    return null
  }
}

function decodeToken(token) {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decoded = JSON.parse(atob(normalized))
    const role = decoded.role?.replace(/^ROLE_/, '')
    return {
      id: decoded.sub,
      role,
      rawRole: decoded.role,
      expiresAt: decoded.exp ? decoded.exp * 1000 : null,
    }
  } catch {
    return { id: null, role: null, expiresAt: null }
  }
}

function buildErrorMessage(errorBody, status) {
  if (!errorBody) return `Erro ${status || ''}`.trim()
  if (typeof errorBody === 'string') return errorBody
  return (
    errorBody.detalhes ||
    errorBody.erro ||
    errorBody.message ||
    errorBody.mensagem ||
    `Erro ${status || ''}`.trim()
  )
}

function formatDate(value) {
  if (!value) return 'Sem data'
  const normalized = value.includes('T') ? value : `${value}T00:00:00`
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(new Date(normalized))
}

function formatDateTime(value) {
  if (!value) return 'Sem horario'
  return new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function normalizeDateTime(value) {
  if (!value) return ''
  return value.length === 16 ? `${value}:00` : value
}

function App() {
  const [session, setSession] = useState(readStoredSession)
  const [loginForm, setLoginForm] = useState(emptyLogin)
  const [registerForm, setRegisterForm] = useState(emptyRegister)
  const [authMode, setAuthMode] = useState('login')
  const [events, setEvents] = useState([])
  const [eventStatus, setEventStatus] = useState('ABERTO')
  const [selectedEventId, setSelectedEventId] = useState('')
  const [turmasByEvent, setTurmasByEvent] = useState({})
  const [myInscriptions, setMyInscriptions] = useState([])
  const [adminDashboard, setAdminDashboard] = useState(null)
  const [studentDashboard, setStudentDashboard] = useState(null)
  const [eventForm, setEventForm] = useState(emptyEvent)
  const [turmaForm, setTurmaForm] = useState(emptyTurma)
  const [presenceTurmaId, setPresenceTurmaId] = useState('')
  const [presenceRows, setPresenceRows] = useState([])
  const [professors, setProfessors] = useState([])
  const [loading, setLoading] = useState(false)
  const [notice, setNotice] = useState(null)
  const navigate = useNavigate()

  const role = session?.user?.role
  const selectedEvent = useMemo(
    () => events.find((event) => event.id === selectedEventId) || null,
    [events, selectedEventId],
  )
  const selectedEventTurmas = selectedEventId ? turmasByEvent[selectedEventId] || [] : []

  async function request(path, options = {}) {
    const buildHeaders = (token) =>
      token && options.auth !== false
        ? { Authorization: `Bearer ${token}`, ...(options.headers || {}) }
        : options.headers

    try {
      const response = await api.request({
        url: path,
        method: options.method || 'GET',
        data: options.body,
        headers: buildHeaders(session?.accessToken),
      })
      return response.status === 204 ? null : response.data
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401 && !options._retry) {
        const stored = readStoredSession()
        if (stored?.refreshToken) {
          try {
            const { data } = await api.post('/auth/refresh', { refreshToken: stored.refreshToken })
            const newSession = { ...data, user: decodeToken(data.accessToken) }
            localStorage.setItem(SESSION_KEY, JSON.stringify(data))
            setSession(newSession)
            const retry = await api.request({
              url: path,
              method: options.method || 'GET',
              data: options.body,
              headers: buildHeaders(data.accessToken),
            })
            return retry.status === 204 ? null : retry.data
          } catch {
            localStorage.removeItem(SESSION_KEY)
            setSession(null)
            navigate('/eventos')
            return undefined
          }
        }
      }
      if (axios.isAxiosError(error)) {
        throw new Error(buildErrorMessage(error.response?.data, error.response?.status))
      }
      throw error
    }
  }

  function showNotice(type, message) {
    setNotice({ type, message })
  }

  async function withLoading(action, successMessage) {
    setLoading(true)
    setNotice(null)
    try {
      const result = await action()
      if (successMessage) showNotice('success', successMessage)
      return result
    } catch (error) {
      showNotice('error', error.message || 'Nao foi possivel concluir a acao.')
      return undefined
    } finally {
      setLoading(false)
    }
  }

  async function loadEvents() {
    const statusParam = eventStatus ? `status=${eventStatus}&` : ''
    const data = await request(`/eventos?${statusParam}page=0&size=30`, { auth: false })
    const content = data?.content || []
    setEvents(content)
    if (content.length > 0 && !content.some((event) => event.id === selectedEventId)) {
      setSelectedEventId(content[0].id)
    } else if (content.length === 0) {
      setSelectedEventId('')
    }
    return content
  }

  async function loadTurmas(eventId) {
    if (!eventId) return []
    const data = await request(`/eventos/${eventId}/turmas`, { auth: false })
    setTurmasByEvent((current) => ({ ...current, [eventId]: data || [] }))
    return data || []
  }

  async function loadMyArea() {
    if (role !== 'ALUNO') return
    const [dashboard, inscriptions] = await Promise.all([
      request('/dashboard/aluno'),
      request('/inscricoes/minhas'),
    ])
    setStudentDashboard(dashboard)
    setMyInscriptions(inscriptions || [])
  }

  async function loadAdminDashboard() {
    if (role !== 'ADMIN') return
    const data = await request('/dashboard/admin')
    setAdminDashboard(data)
  }

  async function loadProfessors() {
    if (role !== 'ADMIN') return
    const data = await request('/usuarios/professores')
    if (data) setProfessors(data)
  }

  useEffect(() => {
    withLoading(loadEvents)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [eventStatus])

  useEffect(() => {
    if (selectedEventId && !turmasByEvent[selectedEventId]) {
      withLoading(() => loadTurmas(selectedEventId))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedEventId])

  useEffect(() => {
    if (!session) return
    if (role === 'ALUNO') withLoading(loadMyArea)
    if (role === 'ADMIN') {
      withLoading(loadAdminDashboard)
      withLoading(loadProfessors)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session?.accessToken])

  async function handleLogin(event) {
    event.preventDefault()
    const data = await withLoading(
      () => request('/auth/login', { method: 'POST', body: loginForm, auth: false }),
      'Login realizado.',
    )
    if (!data) return
    const nextSession = { ...data, user: decodeToken(data.accessToken) }
    localStorage.setItem(SESSION_KEY, JSON.stringify(data))
    setSession(nextSession)
    navigate('/eventos')
    setLoginForm(emptyLogin)
  }

  async function handleRegister(event) {
    event.preventDefault()
    const created = await withLoading(
      () => request('/auth/register', { method: 'POST', body: registerForm, auth: false }),
      'Cadastro criado. Agora faca login.',
    )
    if (created !== undefined) {
      setAuthMode('login')
      setLoginForm({ email: registerForm.email, senha: '' })
      setRegisterForm(emptyRegister)
    }
  }

  async function handleLogout() {
    await withLoading(async () => {
      if (session?.accessToken) {
        try {
          await request('/auth/logout', { method: 'POST' })
        } catch {
          // Local cleanup still matters if the token has already expired.
        }
      }
      localStorage.removeItem(SESSION_KEY)
      setSession(null)
      navigate('/eventos')
      setMyInscriptions([])
      setStudentDashboard(null)
      setAdminDashboard(null)
    }, 'Sessao encerrada.')
  }

  async function handleCreateEvent(event) {
    event.preventDefault()
    const payload = {
      ...eventForm,
      cargaHoraria: eventForm.cargaHoraria ? Number(eventForm.cargaHoraria) : null,
      dataFim: eventForm.dataFim || null,
    }
    const created = await withLoading(
      () => request('/eventos', { method: 'POST', body: payload }),
      'Evento criado.',
    )
    if (!created) return
    setEventForm(emptyEvent)
    await loadEvents()
  }

  async function handleCreateTurma(event) {
    event.preventDefault()
    const payload = {
      nome: turmaForm.nome,
      dataHoraInicio: normalizeDateTime(turmaForm.dataHoraInicio),
      dataHoraFim: normalizeDateTime(turmaForm.dataHoraFim),
      vagas: Number(turmaForm.vagas),
      professorId: turmaForm.professorId,
    }
    const created = await withLoading(
      () => request(`/eventos/${turmaForm.eventoId}/turmas`, { method: 'POST', body: payload }),
      'Turma criada.',
    )
    if (!created) return
    setTurmaForm({ ...emptyTurma, eventoId: turmaForm.eventoId })
    await loadTurmas(turmaForm.eventoId)
  }

  async function handleCancelEvent(eventId) {
    const done = await withLoading(
      () => request(`/eventos/${eventId}`, { method: 'DELETE' }),
      'Evento cancelado.',
    )
    if (done === null) await loadEvents()
  }

  async function handleCancelTurma(turmaId, eventId) {
    const done = await withLoading(
      () => request(`/turmas/${turmaId}`, { method: 'DELETE' }),
      'Turma cancelada.',
    )
    if (done === null) await loadTurmas(eventId)
  }

  async function handleInscrever(turmaId) {
    const result = await withLoading(
      () => request(`/turmas/${turmaId}/inscricoes`, { method: 'POST' }),
      'Inscricao enviada.',
    )
    if (!result) return
    await loadMyArea()
  }

  async function handleCancelInscription(inscricaoId) {
    const done = await withLoading(
      () => request(`/inscricoes/${inscricaoId}`, { method: 'DELETE' }),
      'Inscricao cancelada.',
    )
    if (done === null) await loadMyArea()
  }

  async function loadPresence(turmaId = presenceTurmaId) {
    if (!turmaId) {
      showNotice('error', 'Informe uma turma para consultar presenca.')
      return
    }
    const rows = await withLoading(() => request(`/turmas/${turmaId}/presenca`))
    if (rows) setPresenceRows(rows)
  }

  async function savePresence() {
    if (!presenceTurmaId || presenceRows.length === 0) return
    const payload = {
      presencas: presenceRows.map((row) => ({
        inscricaoId: row.inscricaoId,
        presente: Boolean(row.presente),
      })),
    }
    const done = await withLoading(
      () => request(`/turmas/${presenceTurmaId}/presenca`, { method: 'PUT', body: payload }),
      'Presenca salva.',
    )
    if (done !== undefined) await loadPresence(presenceTurmaId)
  }

  function fillDemoAccount(account) {
    setAuthMode('login')
    setLoginForm({ email: account.email, senha: account.senha })
  }

  function updatePresence(inscricaoId, presente) {
    setPresenceRows((rows) =>
      rows.map((row) => (row.inscricaoId === inscricaoId ? { ...row, presente } : row)),
    )
  }

  const canManageEvents = role === 'ADMIN'
  const canManagePresence = role === 'ADMIN' || role === 'PROFESSOR'

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Navegacao principal">
        <div className="brand">
          <span className="brand-mark">EF</span>
          <div>
            <strong>EventFlow</strong>
            <small>Gestao academica</small>
          </div>
        </div>

        <nav className="nav-list">
          <NavLink to="/eventos">
            Eventos
          </NavLink>
          {role === 'ALUNO' && (
            <NavLink to="/minhas-inscricoes">
              Minhas inscricoes
            </NavLink>
          )}
          {canManageEvents && (
            <NavLink to="/admin">
              Admin
            </NavLink>
          )}
          {canManagePresence && (
            <NavLink to="/presenca">
              Presenca
            </NavLink>
          )}
        </nav>

        <section className="auth-panel">
          {session ? (
            <>
              <span className="eyebrow">Sessao ativa</span>
              <strong>{role || 'Usuario'}</strong>
              <small>{session.user?.id}</small>
              <button className="secondary" onClick={handleLogout} disabled={loading}>
                Sair
              </button>
            </>
          ) : (
            <AuthBox
              authMode={authMode}
              setAuthMode={setAuthMode}
              loginForm={loginForm}
              setLoginForm={setLoginForm}
              registerForm={registerForm}
              setRegisterForm={setRegisterForm}
              handleLogin={handleLogin}
              handleRegister={handleRegister}
              fillDemoAccount={fillDemoAccount}
              loading={loading}
            />
          )}
        </section>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <span className="eyebrow">Projeto P09-B</span>
            <h1>Sistema simples para eventos, turmas e inscricoes</h1>
          </div>
          <StatusPill role={role} />
        </header>

        {notice && (
          <div className={`notice ${notice.type}`} role="status">
            {notice.message}
          </div>
        )}

        <Routes>
          <Route path="/" element={<Navigate to="/eventos" replace />} />
          <Route
            path="/eventos"
            element={
              <EventView
                events={events}
                eventStatus={eventStatus}
                setEventStatus={setEventStatus}
                selectedEvent={selectedEvent}
                selectedEventId={selectedEventId}
                setSelectedEventId={setSelectedEventId}
                turmas={selectedEventTurmas}
                loadTurmas={loadTurmas}
                role={role}
                canManageEvents={canManageEvents}
                handleInscrever={handleInscrever}
                handleCancelEvent={handleCancelEvent}
                handleCancelTurma={handleCancelTurma}
                setPresenceTurmaId={setPresenceTurmaId}
                loading={loading}
              />
            }
          />
          <Route
            path="/minhas-inscricoes"
            element={
              role === 'ALUNO' ? (
                <StudentView
                  dashboard={studentDashboard}
                  inscriptions={myInscriptions}
                  handleCancelInscription={handleCancelInscription}
                  refresh={loadMyArea}
                  loading={loading}
                />
              ) : (
                <Navigate to="/eventos" replace />
              )
            }
          />
          <Route
            path="/admin"
            element={
              canManageEvents ? (
                <AdminView
                  dashboard={adminDashboard}
                  events={events}
                  professors={professors}
                  eventForm={eventForm}
                  setEventForm={setEventForm}
                  turmaForm={turmaForm}
                  setTurmaForm={setTurmaForm}
                  handleCreateEvent={handleCreateEvent}
                  handleCreateTurma={handleCreateTurma}
                  refreshDashboard={loadAdminDashboard}
                  loading={loading}
                />
              ) : (
                <Navigate to="/eventos" replace />
              )
            }
          />
          <Route
            path="/presenca"
            element={
              canManagePresence ? (
                <PresenceView
                  events={events}
                  turmasByEvent={turmasByEvent}
                  loadTurmas={loadTurmas}
                  presenceTurmaId={presenceTurmaId}
                  setPresenceTurmaId={setPresenceTurmaId}
                  presenceRows={presenceRows}
                  updatePresence={updatePresence}
                  loadPresence={loadPresence}
                  savePresence={savePresence}
                  loading={loading}
                />
              ) : (
                <Navigate to="/eventos" replace />
              )
            }
          />
          <Route path="*" element={<Navigate to="/eventos" replace />} />
        </Routes>
      </main>
    </div>
  )
}

function AuthBox({
  authMode,
  setAuthMode,
  loginForm,
  setLoginForm,
  registerForm,
  setRegisterForm,
  handleLogin,
  handleRegister,
  fillDemoAccount,
  loading,
}) {
  return (
    <>
      <div className="segmented" role="tablist" aria-label="Autenticacao">
        <button className={authMode === 'login' ? 'active' : ''} onClick={() => setAuthMode('login')}>
          Login
        </button>
        <button
          className={authMode === 'register' ? 'active' : ''}
          onClick={() => setAuthMode('register')}
        >
          Cadastro
        </button>
      </div>

      {authMode === 'login' ? (
        <form className="stack" onSubmit={handleLogin}>
          <label>
            Email
            <input
              type="email"
              value={loginForm.email}
              onChange={(event) => setLoginForm({ ...loginForm, email: event.target.value })}
              required
            />
          </label>
          <label>
            Senha
            <input
              type="password"
              value={loginForm.senha}
              onChange={(event) => setLoginForm({ ...loginForm, senha: event.target.value })}
              required
            />
          </label>
          <button type="submit" disabled={loading}>
            Entrar
          </button>
        </form>
      ) : (
        <form className="stack" onSubmit={handleRegister}>
          <label>
            Nome
            <input
              value={registerForm.nome}
              onChange={(event) => setRegisterForm({ ...registerForm, nome: event.target.value })}
              required
            />
          </label>
          <label>
            Email
            <input
              type="email"
              value={registerForm.email}
              onChange={(event) => setRegisterForm({ ...registerForm, email: event.target.value })}
              required
            />
          </label>
          <label>
            Senha
            <input
              type="password"
              minLength={8}
              value={registerForm.senha}
              onChange={(event) => setRegisterForm({ ...registerForm, senha: event.target.value })}
              required
            />
          </label>
          <button type="submit" disabled={loading}>
            Criar conta
          </button>
        </form>
      )}

      <div className="demo-list">
        <span className="eyebrow">Acesso demo</span>
        {demoAccounts.map((account) => (
          <button
            className="ghost"
            key={account.email}
            onClick={() => fillDemoAccount(account)}
            type="button"
          >
            {account.role}
          </button>
        ))}
      </div>
    </>
  )
}

function EventView({
  events,
  eventStatus,
  setEventStatus,
  selectedEvent,
  selectedEventId,
  setSelectedEventId,
  turmas,
  loadTurmas,
  role,
  canManageEvents,
  handleInscrever,
  handleCancelEvent,
  handleCancelTurma,
  setPresenceTurmaId,
  loading,
}) {
  const navigate = useNavigate()
  return (
    <section className="workspace two-columns">
      <div className="panel">
        <div className="panel-header">
          <div>
            <span className="eyebrow">Grade publica</span>
            <h2>Eventos disponiveis</h2>
          </div>
          <select value={eventStatus} onChange={(event) => setEventStatus(event.target.value)}>
            <option value="">Todos</option>
            <option value="ABERTO">Abertos</option>
            <option value="ENCERRADO">Encerrados</option>
            <option value="CANCELADO">Cancelados</option>
          </select>
        </div>

        <div className="list">
          {events.length === 0 && <EmptyState title="Nenhum evento encontrado" />}
          {events.map((event) => (
            <button
              className={`event-card ${event.id === selectedEventId ? 'selected' : ''}`}
              key={event.id}
              onClick={() => setSelectedEventId(event.id)}
            >
              <span className={`badge ${event.status?.toLowerCase()}`}>{event.status}</span>
              <strong>{event.titulo}</strong>
              <span>{event.local || 'Local nao informado'}</span>
              <small>
                {formatDate(event.dataInicio)}
                {event.dataFim ? ` ate ${formatDate(event.dataFim)}` : ''}
              </small>
            </button>
          ))}
        </div>
      </div>

      <div className="panel detail-panel">
        {!selectedEvent ? (
          <EmptyState title="Selecione um evento" />
        ) : (
          <>
            <div className="panel-header">
              <div>
                <span className="eyebrow">{selectedEvent.status}</span>
                <h2>{selectedEvent.titulo}</h2>
              </div>
              {canManageEvents && (
                <button
                  className="danger"
                  onClick={() => handleCancelEvent(selectedEvent.id)}
                  disabled={loading || selectedEvent.status === 'CANCELADO'}
                >
                  Cancelar
                </button>
              )}
            </div>

            <p className="description">
              {selectedEvent.descricao || 'Este evento ainda nao possui descricao.'}
            </p>

            <div className="meta-grid">
              <Meta label="Periodo" value={`${formatDate(selectedEvent.dataInicio)} - ${formatDate(selectedEvent.dataFim)}`} />
              <Meta label="Local" value={selectedEvent.local || 'Nao informado'} />
              <Meta label="Carga" value={selectedEvent.cargaHoraria ? `${selectedEvent.cargaHoraria}h` : 'Nao informada'} />
            </div>

            <div className="panel-header compact">
              <h3>Turmas</h3>
              <button className="secondary" onClick={() => loadTurmas(selectedEvent.id)} disabled={loading}>
                Atualizar
              </button>
            </div>

            <div className="table-like">
              {turmas.length === 0 && <EmptyState title="Nenhuma turma cadastrada" />}
              {turmas.map((turma) => (
                <div className="row-card" key={turma.id}>
                  <div>
                    <strong>{turma.nome}</strong>
                    <small>{turma.professorNome || 'Professor nao informado'}</small>
                  </div>
                  <div>
                    <span>{formatDateTime(turma.dataHoraInicio)}</span>
                    <small>{formatDateTime(turma.dataHoraFim)}</small>
                  </div>
                  <div>
                    <span>{turma.vagas} vagas</span>
                    <small>{turma.status}</small>
                  </div>
                  <div className="actions">
                    {role === 'ALUNO' && turma.status === 'ABERTA' && (
                      <button onClick={() => handleInscrever(turma.id)} disabled={loading}>
                        Inscrever
                      </button>
                    )}
                    {(role === 'ADMIN' || role === 'PROFESSOR') && (
                      <button
                        className="secondary"
                        onClick={() => {
                          setPresenceTurmaId(turma.id)
                          navigate('/presenca')
                        }}
                      >
                        Presenca
                      </button>
                    )}
                    {canManageEvents && (
                      <button
                        className="danger"
                        onClick={() => handleCancelTurma(turma.id, selectedEvent.id)}
                        disabled={loading || turma.status === 'CANCELADA'}
                      >
                        Cancelar
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </section>
  )
}

function StudentView({ dashboard, inscriptions, handleCancelInscription, refresh, loading }) {
  return (
    <section className="workspace">
      <div className="panel">
        <div className="panel-header">
          <div>
            <span className="eyebrow">Aluno</span>
            <h2>Minhas inscricoes</h2>
          </div>
          <button className="secondary" onClick={refresh} disabled={loading}>
            Atualizar
          </button>
        </div>

        <StatsGrid
          items={[
            ['Inscricoes', dashboard?.totalInscricoes ?? 0],
            ['Turmas concluidas', dashboard?.turmasConcluidas ?? 0],
            ['Presencas', dashboard?.presencasRegistradas ?? 0],
          ]}
        />

        <div className="table-like">
          {inscriptions.length === 0 && <EmptyState title="Voce ainda nao possui inscricoes" />}
          {inscriptions.map((inscricao) => (
            <div className="row-card" key={inscricao.id}>
              <div>
                <strong>{inscricao.eventoTitulo}</strong>
                <small>{inscricao.turmaNome}</small>
              </div>
              <div>
                <span>{inscricao.status}</span>
                {inscricao.posicaoEspera && <small>Posicao {inscricao.posicaoEspera}</small>}
              </div>
              <div>
                <span>{inscricao.presente ? 'Presente' : 'Presenca pendente'}</span>
                <small>{formatDateTime(inscricao.criadaEm)}</small>
              </div>
              <div className="actions">
                <button
                  className="danger"
                  onClick={() => handleCancelInscription(inscricao.id)}
                  disabled={loading || inscricao.status === 'CANCELADA'}
                >
                  Cancelar
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

function AdminView({
  dashboard,
  events,
  professors,
  eventForm,
  setEventForm,
  turmaForm,
  setTurmaForm,
  handleCreateEvent,
  handleCreateTurma,
  refreshDashboard,
  loading,
}) {
  return (
    <section className="workspace admin-grid">
      <div className="panel full-span">
        <div className="panel-header">
          <div>
            <span className="eyebrow">Admin</span>
            <h2>Visao geral</h2>
          </div>
          <button className="secondary" onClick={refreshDashboard} disabled={loading}>
            Atualizar
          </button>
        </div>
        <StatsGrid
          items={[
            ['Eventos', dashboard?.totalEventos ?? 0],
            ['Turmas', dashboard?.totalTurmas ?? 0],
            ['Inscricoes', dashboard?.totalInscricoes ?? 0],
            ['Alunos', dashboard?.totalAlunos ?? 0],
            ['Ocupacao', `${Math.round(dashboard?.taxaOcupacao ?? 0)}%`],
          ]}
        />
      </div>

      <form className="panel form-panel" onSubmit={handleCreateEvent}>
        <div>
          <span className="eyebrow">Cadastro</span>
          <h2>Novo evento</h2>
        </div>
        <label>
          Titulo
          <input
            value={eventForm.titulo}
            onChange={(event) => setEventForm({ ...eventForm, titulo: event.target.value })}
            required
          />
        </label>
        <label>
          Descricao
          <textarea
            value={eventForm.descricao}
            onChange={(event) => setEventForm({ ...eventForm, descricao: event.target.value })}
            rows={4}
          />
        </label>
        <div className="form-grid">
          <label>
            Inicio
            <input
              type="date"
              value={eventForm.dataInicio}
              onChange={(event) => setEventForm({ ...eventForm, dataInicio: event.target.value })}
              required
            />
          </label>
          <label>
            Fim
            <input
              type="date"
              value={eventForm.dataFim}
              onChange={(event) => setEventForm({ ...eventForm, dataFim: event.target.value })}
            />
          </label>
        </div>
        <div className="form-grid">
          <label>
            Local
            <input
              value={eventForm.local}
              onChange={(event) => setEventForm({ ...eventForm, local: event.target.value })}
            />
          </label>
          <label>
            Carga horaria
            <input
              type="number"
              min="1"
              value={eventForm.cargaHoraria}
              onChange={(event) => setEventForm({ ...eventForm, cargaHoraria: event.target.value })}
            />
          </label>
        </div>
        <button type="submit" disabled={loading}>
          Criar evento
        </button>
      </form>

      <form className="panel form-panel" onSubmit={handleCreateTurma}>
        <div>
          <span className="eyebrow">Cadastro</span>
          <h2>Nova turma</h2>
        </div>
        <label>
          Evento
          <select
            value={turmaForm.eventoId}
            onChange={(event) => setTurmaForm({ ...turmaForm, eventoId: event.target.value })}
            required
          >
            <option value="">Selecione</option>
            {events.map((event) => (
              <option value={event.id} key={event.id}>
                {event.titulo}
              </option>
            ))}
          </select>
        </label>
        <label>
          Nome da turma
          <input
            value={turmaForm.nome}
            onChange={(event) => setTurmaForm({ ...turmaForm, nome: event.target.value })}
            required
          />
        </label>
        <div className="form-grid">
          <label>
            Inicio
            <input
              type="datetime-local"
              value={turmaForm.dataHoraInicio}
              onChange={(event) => setTurmaForm({ ...turmaForm, dataHoraInicio: event.target.value })}
              required
            />
          </label>
          <label>
            Fim
            <input
              type="datetime-local"
              value={turmaForm.dataHoraFim}
              onChange={(event) => setTurmaForm({ ...turmaForm, dataHoraFim: event.target.value })}
              required
            />
          </label>
        </div>
        <div className="form-grid">
          <label>
            Vagas
            <input
              type="number"
              min="1"
              max="500"
              value={turmaForm.vagas}
              onChange={(event) => setTurmaForm({ ...turmaForm, vagas: event.target.value })}
              required
            />
          </label>
          <label>
            Professor
            <select
              value={turmaForm.professorId}
              onChange={(event) => setTurmaForm({ ...turmaForm, professorId: event.target.value })}
              required
            >
              <option value="">Selecione</option>
              {professors.map((p) => (
                <option value={p.id} key={p.id}>{p.nome}</option>
              ))}
            </select>
          </label>
        </div>
        <button type="submit" disabled={loading}>
          Criar turma
        </button>
      </form>
    </section>
  )
}

function PresenceView({
  events,
  turmasByEvent,
  loadTurmas,
  presenceTurmaId,
  setPresenceTurmaId,
  presenceRows,
  updatePresence,
  loadPresence,
  savePresence,
  loading,
}) {
  const [eventId, setEventId] = useState('')
  const turmas = eventId ? turmasByEvent[eventId] || [] : []

  async function handleEventChange(nextEventId) {
    setEventId(nextEventId)
    setPresenceTurmaId('')
    if (nextEventId && !turmasByEvent[nextEventId]) {
      await loadTurmas(nextEventId)
    }
  }

  return (
    <section className="workspace">
      <div className="panel">
        <div className="panel-header">
          <div>
            <span className="eyebrow">Professor/Admin</span>
            <h2>Controle de presenca</h2>
          </div>
          <button className="secondary" onClick={() => loadPresence()} disabled={loading || !presenceTurmaId}>
            Consultar
          </button>
        </div>

        <div className="form-grid three">
          <label>
            Evento
            <select value={eventId} onChange={(event) => handleEventChange(event.target.value)}>
              <option value="">Selecione</option>
              {events.map((event) => (
                <option value={event.id} key={event.id}>
                  {event.titulo}
                </option>
              ))}
            </select>
          </label>
          <label>
            Turma
            <select
              value={presenceTurmaId}
              onChange={(event) => setPresenceTurmaId(event.target.value)}
            >
              <option value="">Selecione</option>
              {turmas.map((turma) => (
                <option value={turma.id} key={turma.id}>
                  {turma.nome}
                </option>
              ))}
            </select>
          </label>
          <label>
            Turma por ID
            <input
              value={presenceTurmaId}
              onChange={(event) => setPresenceTurmaId(event.target.value)}
              placeholder="UUID da turma"
            />
          </label>
        </div>

        <div className="table-like">
          {presenceRows.length === 0 && <EmptyState title="Consulte uma turma para listar alunos" />}
          {presenceRows.map((row) => (
            <div className="row-card" key={row.inscricaoId}>
              <div>
                <strong>{row.alunoNome}</strong>
                <small>{row.status}</small>
              </div>
              <div className="toggle-group" aria-label={`Presenca de ${row.alunoNome}`}>
                <button
                  className={row.presente ? 'active' : ''}
                  onClick={() => updatePresence(row.inscricaoId, true)}
                  type="button"
                >
                  Presente
                </button>
                <button
                  className={row.presente === false ? 'active danger-soft' : ''}
                  onClick={() => updatePresence(row.inscricaoId, false)}
                  type="button"
                >
                  Ausente
                </button>
              </div>
            </div>
          ))}
        </div>

        {presenceRows.length > 0 && (
          <div className="footer-actions">
            <button onClick={savePresence} disabled={loading}>
              Salvar presenca
            </button>
          </div>
        )}
      </div>
    </section>
  )
}

function StatusPill({ role }) {
  return (
    <div className="status-pill">
      <span>{role ? `Perfil ${role}` : 'Acesso publico'}</span>
    </div>
  )
}

function StatsGrid({ items }) {
  return (
    <div className="stats-grid">
      {items.map(([label, value]) => (
        <div className="stat-card" key={label}>
          <strong>{value}</strong>
          <span>{label}</span>
        </div>
      ))}
    </div>
  )
}

function Meta({ label, value }) {
  return (
    <div className="meta-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function EmptyState({ title }) {
  return <div className="empty-state">{title}</div>
}

export default App
