(() => {
  const authScreen = document.querySelector("#auth-screen");
  const workspace = document.querySelector("#workspace");
  const content = document.querySelector("#content");
  const navigation = document.querySelector("#navigation");
  const toast = document.querySelector("#toast");
  const state = { user: null, accessToken: "", refreshPromise: null, currentView: "overview", reviewId: null, toastTimer: null };
  const studentNav = [
    ["overview", "Overview", "⌂"], ["profile", "My profile", "◉"], ["assessment", "Assessment", "◎"],
    ["skills", "Skills", "⌁"], ["careers", "Career matches", "↗"], ["gaps", "Skill gaps", "◫"],
    ["roadmap", "My roadmap", "⌖"], ["resources", "Resources", "▤"], ["feedback", "Mentor notes", "✳"]
  ];
  const mentorNav = [["mentor", "Assigned students", "◉"]];
  const adminNav = [["admin-users", "Users & roles", "◉"], ["admin-roles", "Career roles", "↗"],
    ["admin-skills", "Skill catalog", "⌁"], ["admin-resources", "Resources", "▤"], ["admin-reports", "Reports", "▥"]];
  const viewTitles = {
    overview: "Overview", profile: "My profile", assessment: "Assessment", skills: "Skills",
    careers: "Career matches", gaps: "Skill-gap analysis", roadmap: "Learning roadmap", resources: "Resources",
    feedback: "Mentor notes", mentor: "Assigned students", "admin-users": "Users & roles", "admin-roles": "Career roles",
    "admin-skills": "Skill catalog", "admin-resources": "Resource catalog", "admin-reports": "Reports"
  };

  const esc = (value) => String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[char]);
  const levelName = (level) => ({ 0: "Not recorded", 1: "Beginner", 2: "Intermediate", 3: "Advanced" })[level] || "Not recorded";
  const roleLabel = (role) => String(role || "").replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
  const initials = (name) => String(name || "CA").split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();

  async function sendApiRequest(url, options = {}) {
    const { skipAuth = false, ...requestOptions } = options;
    const method = (requestOptions.method || "GET").toUpperCase();
    const headers = { Accept: "application/json", ...(requestOptions.headers || {}) };
    if (requestOptions.body !== undefined) headers["Content-Type"] = "application/json";
    if (state.accessToken && !skipAuth) headers.Authorization = `Bearer ${state.accessToken}`;
    const response = await fetch(url, { ...requestOptions, method, headers, credentials: "same-origin" });
    return response;
  }

  async function responseData(response) {
    if (response.status === 204) return null;
    const type = response.headers.get("content-type") || "";
    return type.includes("application/json") ? response.json() : response.text();
  }

  async function refreshAccessToken() {
    if (!state.refreshPromise) {
      state.refreshPromise = (async () => {
        const response = await fetch("/api/auth/refresh", {
          method: "POST",
          headers: { Accept: "application/json" },
          credentials: "same-origin"
        });
        if (!response.ok) return null;
        const session = await responseData(response);
        state.accessToken = session.accessToken;
        return session;
      })().finally(() => { state.refreshPromise = null; });
    }
    return state.refreshPromise;
  }

  async function api(url, options = {}) {
    let response = await sendApiRequest(url, options);
    if (response.status === 401 && !url.startsWith("/api/auth/") && state.accessToken) {
      const session = await refreshAccessToken();
      if (session) {
        state.user = { email: session.email, name: session.name, role: session.role };
        response = await sendApiRequest(url, options);
      } else {
        state.accessToken = "";
        showAuth();
        throw new Error("Your sign-in expired. Please sign in again.");
      }
    }
    const data = await responseData(response);
    if (!response.ok) throw new Error(data?.error || data?.message || `Request failed (${response.status}).`);
    return data;
  }

  function notify(message) {
    toast.textContent = message;
    toast.classList.add("show");
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => toast.classList.remove("show"), 3000);
  }

  function showAuth() {
    state.user = null;
    state.accessToken = "";
    authScreen.classList.remove("hidden");
    workspace.classList.add("hidden");
  }

  function showWorkspace(user) {
    state.user = user;
    authScreen.classList.add("hidden");
    workspace.classList.remove("hidden");
    document.querySelector("#account-name").textContent = user.name;
    document.querySelector("#account-role").textContent = user.role;
    document.querySelector("#account-initials").textContent = initials(user.name);
    renderNavigation();
    navigate(user.role === "MENTOR" ? "mentor" : user.role === "ADMIN" ? "admin-reports" : "overview");
  }

  function renderNavigation() {
    const items = state.user.role === "ADMIN" ? adminNav : state.user.role === "MENTOR" ? mentorNav : studentNav;
    navigation.innerHTML = items.map(([id, label, symbol]) => `<button class="nav-item${state.currentView === id ? " active" : ""}" type="button" data-route="${id}"><span class="nav-symbol" aria-hidden="true">${symbol}</span>${label}</button>`).join("");
  }

  function navigate(view) {
    const items = state.user?.role === "ADMIN" ? adminNav : state.user?.role === "MENTOR" ? mentorNav : studentNav;
    if (!items.some((item) => item[0] === view)) view = items[0][0];
    state.currentView = view;
    history.replaceState(null, "", `#${view}`);
    document.querySelector("#crumb-current").textContent = (viewTitles[view] || view).toUpperCase();
    renderNavigation();
    renderCurrent();
    content.focus({ preventScroll: true });
  }

  function head(title, subtitle, action = "") {
    return `<header class="page-head"><div><p class="eyebrow">${state.user.role === "STUDENT" ? "YOUR CAREER ARC" : roleLabel(state.user.role)}</p><h1>${title}</h1><p>${subtitle}</p></div>${action ? `<div class="page-actions">${action}</div>` : ""}</header>`;
  }

  function section(title, aside = "") {
    return `<div class="section-heading"><h2>${title}</h2>${aside}</div>`;
  }

  function panel(body, extra = "") { return `<section class="panel ${extra}">${body}</section>`; }
  function empty(title, detail, action = "") {
    return `<div class="empty-state"><strong>${title}</strong>${detail}${action ? `<div class="page-actions" style="justify-content:center;margin-top:14px">${action}</div>` : ""}</div>`;
  }
  function routeButton(route, text, style = "button-outline") { return `<button class="button ${style} button-small" type="button" data-route="${route}">${text}</button>`; }
  function tag(text, style = "") { return `<span class="tag ${style}">${esc(text)}</span>`; }
  function statusTag(value) { return `<span class="status status-${esc(String(value).toLowerCase())}">${esc(roleLabel(value))}</span>`; }

  async function renderCurrent() {
    content.innerHTML = `<div class="loading">Loading your workspace</div>`;
    try {
      const renderer = {
        overview: renderDashboard, profile: renderProfile, assessment: renderAssessment, skills: renderSkills,
        careers: renderCareers, gaps: renderGaps, roadmap: renderRoadmap, resources: renderResources,
        feedback: renderFeedback, mentor: renderMentor, "admin-users": renderAdminUsers,
        "admin-roles": renderAdminRoles, "admin-skills": renderAdminSkills,
        "admin-resources": renderAdminResources, "admin-reports": renderAdminReports
      }[state.currentView];
      await renderer();
    } catch (error) {
      content.innerHTML = `${head(viewTitles[state.currentView], "Something interrupted this view.")}<div class="notice error">${esc(error.message)}</div><button class="button button-outline" data-action="refresh" type="button">Try again</button>`;
    }
  }

  async function renderDashboard() {
    const data = await api("/api/student/dashboard");
    const recommendationCards = (data.recommendations || []).map((item) => roleCard(item, data.targetRole?.id)).join("");
    const activeMilestones = data.roadmap?.milestones || [];
    const nextMilestone = activeMilestones.find((item) => item.status !== "COMPLETED");
    content.innerHTML = `${head(`Good to see you, ${esc(state.user.name.split(" ")[0])}.`, "Small, deliberate steps add up. Here is where your career exploration stands.", `<button class="button button-outline" data-route="profile" type="button">Update profile</button>`)}
      <section class="welcome-band"><div><p class="eyebrow">YOUR NEXT STEP</p><h2>${nextMilestone ? esc(nextMilestone.title) : data.targetRole ? `Explore ${esc(data.targetRole.title)}` : "Build a clearer career picture"}</h2><p>${nextMilestone ? esc(nextMilestone.description) : "Complete your profile and assessment to get recommendations shaped around you."}</p></div><div class="welcome-aside"><b>${data.progress || 0}%</b><span>roadmap progress</span><div class="progress-track lime" style="margin-top:11px"><span style="width:${data.progress || 0}%"></span></div></div></section>
      ${(data.alerts || []).map((alert) => `<div class="notice">${esc(alert)}</div>`).join("")}
      <div class="grid grid-4"><div class="stat"><div class="stat-label">PROFILE COMPLETE</div><div class="stat-value">${data.profileCompletion}%</div><div class="stat-note">Your background and goals</div></div><div class="stat"><div class="stat-label">SKILLS ON PROFILE</div><div class="stat-value">${data.skillCount}</div><div class="stat-note">Across technical and soft skills</div></div><div class="stat"><div class="stat-label">ROADMAP MILESTONES</div><div class="stat-value">${data.completedMilestones}<span style="font:400 15px var(--body);color:#8a958d"> / ${data.totalMilestones}</span></div><div class="stat-note">Completed so far</div></div><div class="stat"><div class="stat-label">CURRENT SKILL GAPS</div><div class="stat-value">${(data.gaps || []).filter((gap) => gap.status !== "MET").length}</div><div class="stat-note">For ${esc(data.targetRole?.title || "your chosen role")}</div></div></div>
      ${section("Your strongest matches", routeButton("careers", "View all matches"))}
      ${recommendationCards ? `<div class="grid grid-3">${recommendationCards}</div>` : empty("Recommendations are ready to shape", "Complete your profile, add a few skills, or take the assessment to see your ranked career matches.", routeButton("assessment", "Take the assessment", "button-dark"))}
      <div class="grid grid-2" style="margin-top:18px"><div>${section("Latest assessment")}<div id="dashboard-assessment">${data.assessment ? panel(`<div class="panel-pad"><div class="list-primary">Strongest area: ${esc(roleLabel(data.assessment.strongestArea))}</div><div class="tag-list">${Object.entries(data.assessment.scores || {}).map(([key, score]) => tag(`${roleLabel(key)} ${score}%`, "good")).join("")}</div><div class="list-secondary">${new Date(data.assessment.completedAt).toLocaleDateString()}</div></div>`) : empty("No assessment yet", "Your responses help tune role matches.", routeButton("assessment", "Start assessment", "button-dark"))}</div></div>
      <div>${section("Target career")}<div class="panel panel-pad">${data.targetRole ? `<div class="list-primary">${esc(data.targetRole.title)}</div><p class="list-secondary">${esc(data.targetRole.description || "")}</p>${routeButton("gaps", "Review skill gaps")}` : `${empty("No target selected", "Choose a role to build a focused skill plan.", routeButton("careers", "Explore roles", "button-dark"))}`}</div></div></div>`;
  }

  function roleCard(item, selectedId) {
    const role = item.role || {};
    const selected = selectedId && Number(selectedId) === Number(role.id);
    const reasons = (item.relevantFactors || []).slice(0, 2).map((factor) => `<span class="list-secondary">${esc(factor)}</span>`).join("");
    const chips = (item.matchingSkills || []).slice(0, 4).map((name) => tag(name, "good")).join("");
    return `<article class="role-card${selected ? " selected" : ""}"><div class="role-topline"><span class="role-domain">${esc(role.domain)}</span><span class="match-score" aria-label="${item.score} percent match">${item.score}%</span></div><h3>${esc(role.title)}</h3><p>${esc(role.description || "")}</p>${chips ? `<div class="tag-list">${chips}</div>` : ""}<p class="role-reasons">${reasons || esc(item.explanation || "Based on your current profile.")}</p><div class="page-actions" style="margin-top:13px"><button class="button ${selected ? "button-outline" : "button-dark"} button-small" type="button" data-action="choose-role" data-role-id="${role.id}">${selected ? "Selected target" : "Set as target"}</button><button class="button button-outline button-small" type="button" data-action="role-details" data-role-id="${role.id}">Skill profile</button></div></article>`;
  }

  async function renderProfile() {
    const data = await api("/api/student/profile");
    content.innerHTML = `${head("My profile", "A few grounded details help make your recommendations more relevant.")}${panel(`<form id="profile-form" class="panel-pad"><div class="form-grid"><label class="span-2">Name<input name="name" value="${esc(data.name)}" maxlength="120" required></label><label>Contact email<input value="${esc(data.email)}" disabled></label><label>Phone (optional)<input name="phone" value="${esc(data.phone)}" maxlength="30"></label><label>Education level<select name="educationLevel"><option value="">Choose education level</option>${["High school", "Diploma", "Undergraduate", "Postgraduate", "Other"].map((value) => `<option${data.educationLevel === value ? " selected" : ""}>${value}</option>`).join("")}</select></label><label>Academic background<input name="fieldOfStudy" value="${esc(data.fieldOfStudy)}" maxlength="120" placeholder="e.g. Computer Science"></label><label>Academic performance (0-100)<input name="academicScore" type="number" min="0" max="100" step="0.1" value="${esc(data.academicScore)}"></label><label>Preferred career domains<input name="preferredDomains" value="${esc(data.preferredDomains)}" maxlength="500" placeholder="Software & Data, Product Design"></label><label class="span-2">Interests<input name="interests" value="${esc(data.interests)}" maxlength="500" placeholder="What topics or activities hold your attention?"></label><label class="span-2">Career goals<textarea name="careerGoals" maxlength="1000" placeholder="What would you like to work toward?">${esc(data.careerGoals)}</textarea></label></div><div class="form-actions"><button class="button button-dark" type="submit">Save profile</button></div></form>`)}`;
  }

  async function renderAssessment() {
    const [questions, history] = await Promise.all([api("/api/student/assessment/questions"), api("/api/student/assessment/history")]);
    content.innerHTML = `${head("A little more about how you think", "This project-level guidance aid is not a scientifically validated psychological test.", `<button class="button button-outline" data-action="retake" type="button">Retake assessment</button>`)}<div class="grid grid-2"><div>${panel(`<form id="assessment-form" class="panel-pad"><p class="list-secondary">Rate each statement from 1 (not like me) to 5 (very much like me).</p>${questions.map((item) => `<div class="question"><small>${esc(item.category)}</small><p>${esc(item.prompt)}</p><div class="scale-options">${[1, 2, 3, 4, 5].map((score) => `<label><input type="radio" name="q${item.id}" value="${score}"${score === 3 ? " checked" : ""} required>${score}</label>`).join("")}</div></div>`).join("")}<div class="form-grid" style="margin-top:15px"><label>Interests<input name="interests" required maxlength="500" placeholder="data, design, people"></label><label>Preferred domains<input name="preferredDomains" required maxlength="500" placeholder="Software & Data"></label></div><div class="form-actions"><button class="button button-dark" type="submit">Save assessment</button></div></form>`)}</div><div>${section("Your recent results")}${history.length ? panel(`<div class="panel-pad">${history.map((item) => `<div class="feedback-note"><div class="list-row" style="padding-top:0"><div class="list-primary">Strongest area: ${esc(roleLabel(item.strongestArea))}</div><small>${new Date(item.completedAt).toLocaleDateString()}</small></div><div class="tag-list">${Object.entries(item.scores || {}).map(([key, score]) => tag(`${roleLabel(key)} ${score}%`, score >= 70 ? "good" : "")).join("")}</div><div class="list-secondary">${esc(item.explanation)}</div></div>`).join("")}</div>`) : empty("Results appear here", "Complete the questions to add your first assessment.")}</div></div>`;
  }

  async function renderSkills() {
    const [owned, catalog] = await Promise.all([api("/api/student/skills"), api("/api/catalog/skills")]);
    const ownedIds = new Set(owned.map((item) => Number(item.id)));
    const options = catalog.filter((item) => !ownedIds.has(Number(item.id))).map((item) => `<option value="${item.id}" data-name="${esc(item.name)}" data-category="${esc(item.category)}">${esc(item.name)} · ${esc(roleLabel(item.category))}</option>`).join("");
    content.innerHTML = `${head("Skills you bring", "Record your current proficiency. Role comparisons use the same three-level scale.")}${panel(`<form id="skill-form" class="panel-pad inline-form"><label>Skill<select name="skillId" required><option value="">Choose a skill</option>${options}</select></label><label>Category<select name="category"><option value="TECHNICAL">Technical</option><option value="SOFT">Soft skill</option></select></label><label>Proficiency<select name="proficiency"><option value="1">Beginner</option><option value="2">Intermediate</option><option value="3">Advanced</option></select></label><button class="button button-dark" type="submit">Add or update skill</button></form>`)}${section(`Your skill profile · ${owned.length}`)}${owned.length ? panel(`<div class="panel-pad data-list">${owned.map((item) => `<div class="list-row"><div><div class="list-primary">${esc(item.name)}</div><div class="list-secondary">${esc(roleLabel(item.category))} · ${esc(item.level)}</div></div><div class="page-actions"><label style="display:flex;align-items:center;gap:6px;font-size:10px">Level<select data-action="skill-level" data-skill-id="${item.id}" aria-label="Update ${esc(item.name)} proficiency" style="width:130px;min-height:32px"><option value="1"${item.proficiency === 1 ? " selected" : ""}>Beginner</option><option value="2"${item.proficiency === 2 ? " selected" : ""}>Intermediate</option><option value="3"${item.proficiency === 3 ? " selected" : ""}>Advanced</option></select></label><button class="button button-danger button-small" type="button" data-action="remove-skill" data-skill-id="${item.id}" aria-label="Remove ${esc(item.name)}">Remove</button></div></div>`).join("")}</div>`) : empty("Start with what you know", "Add a few technical or soft skills to make role matching more personal.")}`;
  }

  async function renderCareers() {
    let recommendations = await api("/api/student/recommendations");
    if (!recommendations.length) recommendations = await api("/api/student/recommendations/calculate", { method: "POST" });
    const profile = await api("/api/student/profile");
    content.innerHTML = `${head("Roles with a reason behind them", "Matches combine your skills, assessment, interests, preferences, and academic background.", `<button class="button button-dark" type="button" data-action="recalculate">Recalculate matches <span aria-hidden="true">&#8635;</span></button>`)}<div class="notice">Scores use a transparent rule-based Java matching model. They are a starting point for exploration, not a prediction of hiring outcomes.</div><div class="grid grid-3">${recommendations.map((item) => roleCard(item, profile.targetRole?.id)).join("")}</div>`;
  }

  async function renderGaps() {
    const result = await api("/api/student/skill-gaps");
    if (!result.role?.title) {
      const roles = await api("/api/student/recommendations");
      content.innerHTML = `${head("A practical skills comparison", "Choose a career direction to see where your profile already fits and what to build next.")}${empty("Choose a target career first", "Your gap analysis will compare your recorded levels with that role's requirements.", roles.length ? `<button class="button button-dark button-small" type="button" data-route="careers">Choose from career matches</button>` : `<button class="button button-dark button-small" type="button" data-route="careers">Calculate career matches</button>`)}`;
      return;
    }
    const rows = result.gaps || [];
    const counts = { met: rows.filter((item) => item.status === "MET").length, improve: rows.filter((item) => item.status === "IMPROVE").length, missing: rows.filter((item) => item.status === "MISSING").length };
    content.innerHTML = `${head(`Your fit for ${esc(result.role.title)}`, "Foundations appear first when prerequisite information establishes an order.", `<button class="button button-dark" type="button" data-action="generate-roadmap">Build my learning roadmap <span aria-hidden="true">&#8594;</span></button>`)}<div class="grid grid-3"><div class="stat"><div class="stat-label">MEETING REQUIREMENT</div><div class="stat-value">${counts.met}</div><div class="stat-note">At or above target proficiency</div></div><div class="stat"><div class="stat-label">NEEDS PRACTICE</div><div class="stat-value">${counts.improve}</div><div class="stat-note">Current skills to strengthen</div></div><div class="stat"><div class="stat-label">TO LEARN</div><div class="stat-value">${counts.missing}</div><div class="stat-note">Skills not yet on your profile</div></div></div>${section("Role requirements", tag(result.role.domain))}${panel(`<div class="panel-pad">${rows.map((item) => `<div class="list-row"><div><div class="list-primary">${esc(item.skill)} ${item.prerequisite ? `<span class="list-secondary">Foundation: ${esc(item.prerequisite)}</span>` : ""}</div><div class="list-secondary">Current: ${esc(item.currentLabel)} · Target: ${esc(item.requiredLabel)}</div></div><div>${statusTag(item.status)}<div class="list-secondary" style="text-align:right">Priority ${item.priority === 1 ? "foundational" : "next"}</div></div></div>`).join("")}</div>`)}`;
  }

  async function renderRoadmap() {
    const data = await api("/api/student/roadmap");
    const title = data.roadmap?.role?.title || "Your learning roadmap";
    const items = data.milestones || [];
    content.innerHTML = `${head(esc(title), "A sequence generated from your selected role and current skill gaps.", `<button class="button button-outline" type="button" data-action="generate-roadmap">Regenerate roadmap</button>`)}${items.length ? `${panel(`<div class="panel-pad"><div class="progress-row"><span>Overall progress</span><strong>${data.progress}%</strong></div><div class="progress-track"><span style="width:${data.progress}%"></span></div></div>`, "")}${section(`${items.length} learning milestones`)}${panel(`<div class="panel-pad">${items.map((item) => `<div class="milestone${item.status === "COMPLETED" ? " complete" : ""}"><span class="milestone-number">${item.status === "COMPLETED" ? "✓" : item.sequence}</span><div><h3>${esc(item.title)}</h3><p>${esc(item.description)}${item.skill ? ` · ${esc(item.skill)}` : ""}</p>${item.resource ? `<p style="margin-top:5px"><strong>${esc(item.resource.type)}:</strong> ${esc(item.resource.title)}${item.resource.illustrative ? " · illustrative project resource" : ""}</p>` : ""}</div><select data-action="milestone-status" data-milestone-id="${item.id}" aria-label="Update milestone ${esc(item.title)}"><option value="NOT_STARTED"${item.status === "NOT_STARTED" ? " selected" : ""}>Not started</option><option value="IN_PROGRESS"${item.status === "IN_PROGRESS" ? " selected" : ""}>In progress</option><option value="COMPLETED"${item.status === "COMPLETED" ? " selected" : ""}>Completed</option></select></div>`).join("")}</div>`)}` : empty("No roadmap has been generated", "Select a target career and review your skill gaps first.", `<button class="button button-dark button-small" type="button" data-route="careers">Explore careers</button> <button class="button button-outline button-small" type="button" data-route="gaps">View skill gaps</button>`)}`;
  }

  async function renderResources(filters = {}) {
    const query = new URLSearchParams(Object.entries(filters).filter(([, value]) => value));
    const [items, roles, skills] = await Promise.all([api(`/api/catalog/resources?${query}`), api("/api/catalog/roles"), api("/api/catalog/skills")]);
    const types = [...new Set(items.map((item) => item.type))].sort();
    content.innerHTML = `${head("Learning resources", "Illustrative project resources are stored locally and grouped around skills and career roles.")}${panel(`<form id="resource-filter" class="panel-pad filters"><select name="skill"><option value="">All skills</option>${skills.map((item) => `<option value="${esc(item.name)}"${filters.skill === item.name ? " selected" : ""}>${esc(item.name)}</option>`).join("")}</select><select name="role"><option value="">All careers</option>${roles.map((item) => `<option value="${esc(item.title)}"${filters.role === item.title ? " selected" : ""}>${esc(item.title)}</option>`).join("")}</select><select name="type"><option value="">All types</option>${[...new Set(["COURSE", "PRACTICE", "PROJECT", "CERTIFICATION", ...types])].map((item) => `<option${filters.type === item ? " selected" : ""}>${esc(roleLabel(item))}</option>`).join("")}</select><select name="difficulty"><option value="">All levels</option>${["BEGINNER", "INTERMEDIATE", "ADVANCED"].map((item) => `<option${filters.difficulty === item ? " selected" : ""}>${esc(roleLabel(item))}</option>`).join("")}</select><button class="button button-dark button-small" type="submit">Filter</button><button class="button button-outline button-small" type="button" data-action="clear-filters">Clear</button></form>`)}${items.length ? `<div class="grid grid-3">${items.map((item) => `<article class="resource-card"><span class="role-domain">${esc(roleLabel(item.type))} · ${esc(item.difficulty || "All levels")}</span><h3>${esc(item.title)}</h3><p>${esc(item.description)}${item.illustrative ? " This is an illustrative project resource." : ""}</p><div class="tag-list">${item.skill ? tag(item.skill) : ""}${item.role ? tag(item.role) : ""}${item.url ? `<a class="tag" href="${esc(item.url)}" target="_blank" rel="noreferrer">Open resource ↗</a>` : ""}</div></article>`).join("")}</div>` : empty("No resources match those filters", "Try a different skill, career, type, or difficulty.")}`;
  }

  async function renderFeedback() {
    const items = await api("/api/student/feedback");
    content.innerHTML = `${head("Notes from your mentor", "Feedback is visible to you and the mentor assigned to your account.")}${items.length ? panel(`<div class="panel-pad">${items.map((item) => `<article class="feedback-note"><div class="list-row" style="padding:0;border:0"><div class="list-primary">${esc(item.mentor)}</div><small>${new Date(item.createdAt).toLocaleString()}</small></div><p>${esc(item.message)}</p><div class="tag-list">${item.suggestedRole ? tag(`Career idea: ${item.suggestedRole.title}`, "good") : ""}${item.suggestedResource ? tag(`Resource: ${item.suggestedResource.title}`) : ""}</div></article>`).join("")}</div>`) : empty("No mentor notes yet", "When your assigned mentor shares feedback or alternatives, they will show up here.")}`;
  }

  async function renderMentor() {
    const students = await api("/api/mentor/students");
    if (state.reviewId) {
      const review = await api(`/api/mentor/students/${state.reviewId}`);
      const profile = review.profile;
      const roles = await api("/api/catalog/roles");
      const resources = await api("/api/catalog/resources");
      content.innerHTML = `${head(`Reviewing ${esc(profile.name)}`, "Assigned student profile, recommendations, identified gaps, and real milestone progress.", `<button class="button button-outline" data-action="back-mentor" type="button">All assigned students</button>`)}<div class="grid grid-2"><div>${section("Student profile")}${panel(`<div class="panel-pad"><div class="list-primary">${esc(profile.educationLevel || "Education not entered")} · ${esc(profile.fieldOfStudy || "Field not entered")}</div><div class="list-secondary">Academic score: ${esc(profile.academicScore ?? "Not recorded")}</div><div class="list-secondary">Interests: ${esc(profile.interests || "Not recorded")}</div><div class="list-secondary">Goals: ${esc(profile.careerGoals || "Not recorded")}</div></div>`)}</div><div>${section("Progress snapshot")}${panel(`<div class="panel-pad"><div class="progress-row"><span>Roadmap progress</span><b>${review.dashboard.progress}%</b></div><div class="progress-track"><span style="width:${review.dashboard.progress}%"></span></div><div class="list-secondary" style="margin-top:9px">${review.dashboard.completedMilestones} of ${review.dashboard.totalMilestones} milestones complete</div><div class="list-secondary">Target: ${esc(review.dashboard.targetRole?.title || "Not selected")}</div></div>`)}</div></div>${section("Career recommendations")}${review.recommendations.length ? `<div class="grid grid-3">${review.recommendations.slice(0, 3).map((item) => roleCard(item, profile.targetRole?.id)).join("")}</div>` : empty("No recommendations calculated", "The student can calculate matches from their career workspace.")}${section("Skill gaps and roadmap")}${panel(`<div class="panel-pad"><div class="tag-list">${(review.skillGaps.gaps || []).map((item) => tag(`${item.skill}: ${item.status}`, item.status === "MET" ? "good" : item.status === "MISSING" ? "danger" : "warn")).join("") || "No target role selected"}</div>${(review.roadmap.milestones || []).map((item) => `<div class="list-row"><div><div class="list-primary">${esc(item.title)}</div><div class="list-secondary">${esc(item.description)}</div></div>${statusTag(item.status)}</div>`).join("")}</div>`)}${section("Share guidance")}${panel(`<form id="feedback-form" class="panel-pad" data-student-id="${state.reviewId}"><label>Feedback<textarea name="message" required maxlength="1800" placeholder="Offer a specific, supportive next step."></textarea></label><div class="form-grid" style="margin-top:13px"><label>Suggest a career role<select name="suggestedRoleId"><option value="">No role suggestion</option>${roles.map((item) => `<option value="${item.id}">${esc(item.title)}</option>`).join("")}</select></label><label>Suggest a resource<select name="suggestedResourceId"><option value="">No resource suggestion</option>${resources.map((item) => `<option value="${item.id}">${esc(item.title)}</option>`).join("")}</select></label></div><div class="form-actions"><button class="button button-dark" type="submit">Send feedback</button></div></form>`)}`;
      return;
    }
    content.innerHTML = `${head("Students in your care", "Only students explicitly assigned to your mentor account appear here.")}${students.length ? `<div class="grid grid-2">${students.map((student) => `<article class="panel panel-pad"><div class="role-topline"><span class="role-domain">${esc(student.profile.targetRole?.title || "Exploring careers")}</span><span class="match-score">${student.dashboard.progress}%</span></div><h3 style="font:400 21px var(--display);margin:12px 0 5px">${esc(student.name)}</h3><p class="list-secondary">${esc(student.email)} · ${esc(student.profile.fieldOfStudy || "Academic profile not completed")}</p><p class="list-secondary">${student.dashboard.completedMilestones} / ${student.dashboard.totalMilestones} roadmap milestones complete</p><div class="page-actions"><button class="button button-dark button-small" type="button" data-action="review-student" data-student-id="${student.id}">Review student</button></div></article>`).join("")}</div>` : empty("No students assigned", "Ask an administrator to assign students to your mentor account.")}`;
  }

  async function renderAdminUsers() {
    const [users, assignments] = await Promise.all([api("/api/admin/users"), api("/api/admin/assignments")]);
    const mentors = users.filter((item) => item.role === "MENTOR");
    const students = users.filter((item) => item.role === "STUDENT");
    content.innerHTML = `${head("People and access", "Manage account roles, active access, and explicit mentor-to-student assignments.")}${section("Create an account")}${panel(`<form id="admin-user-form" class="panel-pad"><div class="form-grid"><label>Name<input name="name" required maxlength="120"></label><label>Email<input name="email" type="email" required></label><label>Temporary password<input name="password" type="password" minlength="10" maxlength="72" required></label><label>Role<select name="role"><option>STUDENT</option><option>MENTOR</option><option>ADMIN</option></select></label></div><div class="form-actions"><button class="button button-dark" type="submit">Create account</button></div></form>`)}${section("Assign mentor access")}${panel(`<form id="assignment-form" class="panel-pad inline-form"><label>Mentor<select name="mentorId" required><option value="">Choose mentor</option>${mentors.map((item) => `<option value="${item.id}">${esc(item.name)} · ${esc(item.email)}</option>`).join("")}</select></label><label>Student<select name="studentId" required><option value="">Choose student</option>${students.map((item) => `<option value="${item.id}">${esc(item.name)} · ${esc(item.email)}</option>`).join("")}</select></label><button class="button button-dark" type="submit">Assign student</button></form><div class="panel-pad data-list">${assignments.map((item) => `<div class="list-row"><div><div class="list-primary">${esc(item.mentor)} → ${esc(item.student)}</div><div class="list-secondary">Mentor ID ${item.mentorId} · Student ID ${item.studentId}</div></div><button class="button button-danger button-small" data-action="remove-assignment" data-assignment-id="${item.id}" type="button">Remove</button></div>`).join("") || `<p class="list-secondary">No mentor assignments yet.</p>`}</div>`)}${section(`${users.length} accounts`)}<div class="table-wrap"><table><thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Access</th><th>Save</th></tr></thead><tbody>${users.map((item) => `<tr><td><input data-user-name="${item.id}" value="${esc(item.name)}" aria-label="Name for ${esc(item.email)}"></td><td>${esc(item.email)}</td><td><select data-user-role="${item.id}" aria-label="Role for ${esc(item.email)}">${["STUDENT", "MENTOR", "ADMIN"].map((role) => `<option${item.role === role ? " selected" : ""}>${role}</option>`).join("")}</select></td><td><select data-user-enabled="${item.id}" aria-label="Access for ${esc(item.email)}"><option value="true"${item.enabled ? " selected" : ""}>Enabled</option><option value="false"${!item.enabled ? " selected" : ""}>Disabled</option></select></td><td><button class="button button-outline button-small" data-action="save-user" data-user-id="${item.id}" type="button">Save</button></td></tr>`).join("")}</tbody></table></div>`;
  }

  async function renderAdminRoles() {
    const [roles, skills] = await Promise.all([api("/api/admin/roles"), api("/api/admin/skills")]);
    content.innerHTML = `${head("Career role library", "Maintain role descriptions and proficiency requirements used by matching and gap analysis.")}${section("Add a career role")}${panel(`<form id="admin-role-form" class="panel-pad"><div class="form-grid"><label>Career title<input name="title" required maxlength="120"></label><label>Domain<input name="domain" required maxlength="100"></label><label>Academic fit<input name="academicFit" maxlength="300"></label><label>Related interests<input name="interests" maxlength="400"></label><label class="span-2">Description<textarea name="description" maxlength="1200"></textarea></label></div><div class="form-actions"><button class="button button-dark" type="submit">Add role</button></div></form>`)}${section("Role requirements")}${roles.map((role) => `<details class="panel panel-pad" style="margin-bottom:9px"><summary style="cursor:pointer;font-size:13px;font-weight:750">${esc(role.title)} <span class="list-secondary">· ${esc(role.domain)} · ${role.active ? "Active" : "Inactive"}</span></summary><p class="list-secondary">${esc(role.description)}</p><div class="tag-list">${role.skills.map((item) => tag(`${item.skill} · ${levelName(item.requiredLevel)}`)).join("")}</div><form class="inline-form role-skill-form" data-role-id="${role.id}" style="margin-top:12px"><label>Skill<select name="skillId"><option value="">Choose skill</option>${skills.map((item) => `<option value="${item.id}">${esc(item.name)}</option>`).join("")}</select></label><label>Required level<select name="requiredLevel"><option value="1">Beginner</option><option value="2">Intermediate</option><option value="3">Advanced</option></select></label><button class="button button-outline button-small" type="submit">Add requirement</button></form><form class="role-edit-form" data-role-id="${role.id}" style="margin-top:12px"><div class="form-grid"><label>Title<input name="title" value="${esc(role.title)}" required></label><label>Domain<input name="domain" value="${esc(role.domain)}" required></label><label>Academic fit<input name="academicFit" value="${esc(role.academicFit)}"></label><label>Interests<input name="interests" value="${esc(role.interests)}"></label><label class="span-2">Description<textarea name="description">${esc(role.description)}</textarea></label><label>Active<select name="active"><option value="true"${role.active ? " selected" : ""}>Yes</option><option value="false"${!role.active ? " selected" : ""}>No</option></select></label></div><div class="form-actions"><button class="button button-outline button-small" type="submit">Save role</button></div></form></details>`).join("")}`;
    content.querySelectorAll("details").forEach((details, index) => {
      const role = roles[index];
      if (!role?.skills.length) return;
      const form = document.createElement("form");
      form.className = "inline-form role-skill-delete-form";
      form.dataset.roleId = role.id;
      form.style.marginTop = "9px";
      form.innerHTML = `<label>Remove requirement<select name="linkId">${role.skills.map((item) => `<option value="${item.id}">${esc(item.skill)}</option>`).join("")}</select></label><button class="button button-danger button-small" type="submit">Remove requirement</button>`;
      details.insertBefore(form, details.querySelector(".role-edit-form"));
    });
  }

  async function renderAdminSkills() {
    const skills = await api("/api/admin/skills");
    content.innerHTML = `${head("Skill and prerequisite catalog", "These records power student profiles, role requirements, and learning order.")}${section("Add a skill")}${panel(`<form id="admin-skill-form" class="panel-pad inline-form"><label>Skill name<input name="name" required maxlength="100"></label><label>Category<select name="category"><option>TECHNICAL</option><option>SOFT</option></select></label><label>Prerequisite<select name="prerequisiteId"><option value="">No prerequisite</option>${skills.map((item) => `<option value="${item.id}">${esc(item.name)}</option>`).join("")}</select></label><button class="button button-dark" type="submit">Add skill</button></form>`)}${section(`${skills.length} catalog skills`)}<div class="table-wrap"><table><thead><tr><th>Skill</th><th>Category</th><th>Prerequisite</th><th>Update prerequisite</th><th>Save</th></tr></thead><tbody>${skills.map((item) => `<tr><td>${esc(item.name)}</td><td>${esc(roleLabel(item.category))}</td><td>${esc(item.prerequisite || "None")}</td><td><select data-prerequisite="${item.id}" aria-label="Prerequisite for ${esc(item.name)}"><option value="">None</option>${skills.filter((option) => option.id !== item.id).map((option) => `<option value="${option.id}"${option.name === item.prerequisite ? " selected" : ""}>${esc(option.name)}</option>`).join("")}</select></td><td><button class="button button-outline button-small" type="button" data-action="save-skill-prerequisite" data-skill-id="${item.id}">Save</button></td></tr>`).join("")}</tbody></table></div>`;
  }

  async function renderAdminResources() {
    const [resources, skills, roles] = await Promise.all([api("/api/admin/resources"), api("/api/admin/skills"), api("/api/admin/roles")]);
    content.innerHTML = `${head("Learning resource library", "Add local catalog entries for courses, projects, practice, and certifications.")}${section("Add a learning resource")}${panel(`<form id="admin-resource-form" class="panel-pad"><div class="form-grid"><label>Title<input name="title" required maxlength="180"></label><label>Type<select name="type">${["COURSE", "PRACTICE", "PROJECT", "CERTIFICATION"].map((item) => `<option>${item}</option>`).join("")}</select></label><label>Associated skill<select name="skillId"><option value="">No specific skill</option>${skills.map((item) => `<option value="${item.id}">${esc(item.name)}</option>`).join("")}</select></label><label>Career role<select name="roleId"><option value="">Any role</option>${roles.map((item) => `<option value="${item.id}">${esc(item.title)}</option>`).join("")}</select></label><label>Difficulty<select name="difficulty"><option>BEGINNER</option><option>INTERMEDIATE</option><option>ADVANCED</option></select></label><label>Resource URL (optional)<input name="url" type="url" maxlength="500" placeholder="https://"></label><label class="span-2">Description<textarea name="description" maxlength="1200"></textarea></label><label><span>Illustrative project resource</span><input name="illustrative" type="checkbox" checked style="width:17px;min-height:17px"></label></div><div class="form-actions"><button class="button button-dark" type="submit">Add resource</button></div></form>`)}${section(`${resources.length} resources`)}${resources.length ? `<div class="grid grid-3">${resources.map((item) => `<article class="resource-card"><span class="role-domain">${esc(roleLabel(item.type))} · ${esc(item.difficulty || "All levels")}</span><h3>${esc(item.title)}</h3><p>${esc(item.description)}</p><div class="tag-list">${item.skill ? tag(item.skill) : ""}${item.role ? tag(item.role) : ""}</div></article>`).join("")}</div>` : empty("No resources yet", "Add the first resource above.")}`;
    content.querySelectorAll(".resource-card").forEach((card, index) => {
      const button = document.createElement("button");
      button.type = "button"; button.className = "button button-danger button-small";
      button.dataset.action = "remove-resource"; button.dataset.resourceId = resources[index].id;
      button.textContent = "Remove resource"; button.style.marginTop = "12px";
      card.append(button);
    });
  }

  async function renderAdminReports() {
    const report = await api("/api/admin/reports");
    const counts = [["USERS", report.users], ["STUDENTS", report.students], ["MENTORS", report.mentors], ["CAREER ROLES", report.careerRoles], ["SKILLS", report.skills], ["RESOURCES", report.resources], ["RECOMMENDATION RECORDS", report.recommendationRecords], ["ACTIVE ROADMAPS", report.roadmaps]];
    content.innerHTML = `${head("A view across Career Arc", "Aggregates from persisted local profiles, recommendations, skill-gap analyses, and roadmap milestones.")}${section("Workspace totals")}<div class="grid grid-4">${counts.map(([label, value]) => `<div class="stat"><div class="stat-label">${label}</div><div class="stat-value">${value}</div></div>`).join("")}</div><div class="grid grid-2" style="margin-top:18px"><div>${section("Recommendations by role")}${panel(`<div class="panel-pad data-list">${Object.entries(report.recommendationsByRole || {}).map(([name, count]) => `<div class="list-row"><span class="list-primary">${esc(name)}</span><strong>${count}</strong></div>`).join("") || "<p class=list-secondary>No recommendation records yet.</p>"}</div>`)}</div><div>${section(`Skill gaps · ${report.skillGaps}`)}${panel(`<div class="panel-pad data-list">${Object.entries(report.milestoneStatuses || {}).map(([status, count]) => `<div class="list-row"><span>${statusTag(status)}</span><strong>${count} milestones</strong></div>`).join("") || "<p class=list-secondary>No roadmap milestones yet.</p>"}</div>`)}</div></div>`;
  }

  function formObject(form) {
    const result = {};
    for (const [key, value] of new FormData(form).entries()) result[key] = value;
    return result;
  }

  async function onSubmit(event) {
    const form = event.target;
    event.preventDefault();
    try {
      if (form.id === "login-form") {
        const values = formObject(form);
        const user = await api("/api/auth/login", { method: "POST", body: JSON.stringify(values) });
        state.accessToken = user.accessToken;
        showWorkspace(user);
        return;
      }
      if (form.id === "register-form") {
        const values = formObject(form);
        await api("/api/auth/register", { method: "POST", body: JSON.stringify(values) });
        const user = await api("/api/auth/login", { method: "POST", body: JSON.stringify({ email: values.email, password: values.password }) });
        state.accessToken = user.accessToken;
        notify("Your student profile is ready."); showWorkspace(user); return;
      }
      if (form.id === "profile-form") {
        const values = formObject(form); values.academicScore = values.academicScore ? Number(values.academicScore) : null;
        await api("/api/student/profile", { method: "PUT", body: JSON.stringify(values) }); notify("Profile saved."); navigate("profile"); return;
      }
      if (form.id === "assessment-form") {
        const values = formObject(form);
        const answers = Array.from({ length: 8 }, (_, index) => Number(values[`q${index}`]));
        if (answers.some((value) => !value)) throw new Error("Please answer all eight statements.");
        const result = await api("/api/student/assessment", { method: "POST", body: JSON.stringify({ answers, interests: values.interests, preferredDomains: values.preferredDomains }) });
        notify(`Assessment saved. Strongest area: ${roleLabel(result.strongestArea)}.`); navigate("careers"); return;
      }
      if (form.id === "skill-form") {
        const select = form.elements.skillId; const option = select.selectedOptions[0];
        if (!option?.value) throw new Error("Choose a skill first.");
        await api("/api/student/skills", { method: "POST", body: JSON.stringify({ name: option.dataset.name, category: option.dataset.category || form.elements.category.value, proficiency: Number(form.elements.proficiency.value) }) });
        notify("Skill saved to your profile."); navigate("skills"); return;
      }
      if (form.id === "resource-filter") {
        const values = formObject(form); await renderResources(values); return;
      }
      if (form.id === "feedback-form") {
        const values = formObject(form);
        await api(`/api/mentor/students/${form.dataset.studentId}/feedback`, { method: "POST", body: JSON.stringify({ message: values.message,
          suggestedRoleId: values.suggestedRoleId ? Number(values.suggestedRoleId) : null,
          suggestedResourceId: values.suggestedResourceId ? Number(values.suggestedResourceId) : null }) });
        notify("Feedback shared with the student."); navigate("mentor"); return;
      }
      if (form.classList.contains("role-skill-form")) {
        const values = formObject(form);
        await api(`/api/admin/roles/${form.dataset.roleId}/skills`, { method: "POST", body: JSON.stringify({ skillId: Number(values.skillId), requiredLevel: Number(values.requiredLevel) }) });
        notify("Role requirement saved."); navigate("admin-roles"); return;
      }
      if (form.classList.contains("role-skill-delete-form")) {
        const values = formObject(form);
        await api(`/api/admin/roles/${form.dataset.roleId}/skills/${values.linkId}`, { method: "DELETE" });
        notify("Role requirement removed."); navigate("admin-roles"); return;
      }
      if (form.classList.contains("role-edit-form")) {
        const values = formObject(form); values.active = values.active === "true";
        await api(`/api/admin/roles/${form.dataset.roleId}`, { method: "PUT", body: JSON.stringify(values) }); notify("Career role updated."); navigate("admin-roles"); return;
      }
      if (form.id === "admin-user-form") {
        await api("/api/admin/users", { method: "POST", body: JSON.stringify(formObject(form)) }); notify("Account created."); navigate("admin-users"); return;
      }
      if (form.id === "assignment-form") {
        const values = formObject(form);
        await api("/api/admin/assignments", { method: "POST", body: JSON.stringify({ mentorId: Number(values.mentorId), studentId: Number(values.studentId) }) });
        notify("Student assigned to mentor."); navigate("admin-users"); return;
      }
      if (form.id === "admin-role-form") {
        const values = formObject(form); values.active = true;
        await api("/api/admin/roles", { method: "POST", body: JSON.stringify(values) }); notify("Career role added."); navigate("admin-roles"); return;
      }
      if (form.id === "admin-skill-form") {
        const values = formObject(form); values.prerequisiteId = values.prerequisiteId ? Number(values.prerequisiteId) : null;
        await api("/api/admin/skills", { method: "POST", body: JSON.stringify(values) }); notify("Skill added."); navigate("admin-skills"); return;
      }
      if (form.id === "admin-resource-form") {
        const values = formObject(form); values.skillId = values.skillId ? Number(values.skillId) : null;
        values.roleId = values.roleId ? Number(values.roleId) : null; values.illustrative = form.elements.illustrative.checked;
        await api("/api/admin/resources", { method: "POST", body: JSON.stringify(values) }); notify("Resource added."); navigate("admin-resources");
      }
    } catch (error) {
      const message = form.querySelector(".form-message");
      if (message) message.textContent = error.message;
      else notify(error.message);
    }
  }

  async function onClick(event) {
    const route = event.target.closest("[data-route]");
    if (route) { navigate(route.dataset.route); return; }
    const control = event.target.closest("[data-action]");
    if (!control) return;
    try {
      const action = control.dataset.action;
      if (action === "refresh") { renderCurrent(); return; }
      if (action === "recalculate") { await api("/api/student/recommendations/calculate", { method: "POST" }); notify("Career matches recalculated."); navigate("careers"); }
      if (action === "choose-role") {
        await api("/api/student/target-role", { method: "PUT", body: JSON.stringify({ roleId: Number(control.dataset.roleId) }) });
        notify("Target career selected. Your skill-gap analysis is ready."); navigate("gaps");
      }
      if (action === "role-details") {
        const roles = await api("/api/catalog/roles"); const role = roles.find((item) => Number(item.id) === Number(control.dataset.roleId));
        const requirementList = (role?.requirements || []).map((item) => `<div class="list-row"><div><span class="list-primary">${esc(item.skill)}</span>${item.prerequisite ? `<div class="list-secondary">Prerequisite: ${esc(item.prerequisite)}</div>` : ""}</div>${tag(item.level)}</div>`).join("");
        content.innerHTML = `${head(esc(role.title), esc(role.domain), `<button class="button button-outline" data-route="careers" type="button">Back to matches</button>`)}<p class="muted">${esc(role.description)}</p>${section("Requirements")}${panel(`<div class="panel-pad">${requirementList}</div>`)}`;
      }
      if (action === "generate-roadmap") { await api("/api/student/roadmap/generate", { method: "POST" }); notify("Roadmap generated from your current skill gaps."); navigate("roadmap"); }
      if (action === "remove-skill") { await api(`/api/student/skills/${control.dataset.skillId}`, { method: "DELETE" }); notify("Skill removed."); navigate("skills"); }
      if (action === "recalculate") { await api("/api/student/recommendations/calculate", { method: "POST" }); notify("Matches refreshed."); navigate("careers"); }
      if (action === "clear-filters") navigate("resources");
      if (action === "retake") document.querySelector("#assessment-form")?.scrollIntoView({ behavior: "smooth", block: "start" });
      if (action === "review-student") { state.reviewId = control.dataset.studentId; renderCurrent(); }
      if (action === "back-mentor") { state.reviewId = null; renderCurrent(); }
      if (action === "save-user") {
        const id = control.dataset.userId;
        await api(`/api/admin/users/${id}`, { method: "PUT", body: JSON.stringify({
          name: document.querySelector(`[data-user-name="${id}"]`).value,
          role: document.querySelector(`[data-user-role="${id}"]`).value,
          enabled: document.querySelector(`[data-user-enabled="${id}"]`).value === "true"
        }) }); notify("Account updated."); navigate("admin-users");
      }
      if (action === "remove-assignment") {
        await api(`/api/admin/assignments/${control.dataset.assignmentId}`, { method: "DELETE" });
        notify("Mentor assignment removed."); navigate("admin-users");
      }
      if (action === "remove-resource") {
        await api(`/api/admin/resources/${control.dataset.resourceId}`, { method: "DELETE" });
        notify("Learning resource removed."); navigate("admin-resources");
      }
      if (action === "save-skill-prerequisite") {
        const id = control.dataset.skillId; const catalog = await api("/api/admin/skills"); const item = catalog.find((skill) => Number(skill.id) === Number(id));
        const prerequisiteId = document.querySelector(`[data-prerequisite="${id}"]`).value;
        await api(`/api/admin/skills/${id}`, { method: "PUT", body: JSON.stringify({ name: item.name, category: item.category,
          prerequisiteId: prerequisiteId ? Number(prerequisiteId) : null }) }); notify("Prerequisite updated."); navigate("admin-skills");
      }
    } catch (error) { notify(error.message); }
  }

  async function onChange(event) {
    const control = event.target;
    try {
      if (control.dataset.action === "milestone-status") {
        await api(`/api/student/milestones/${control.dataset.milestoneId}`, { method: "PATCH", body: JSON.stringify({ status: control.value }) });
        notify("Milestone progress saved."); renderCurrent();
      }
      if (control.dataset.action === "skill-level") {
        const catalog = await api("/api/catalog/skills"); const skill = catalog.find((item) => Number(item.id) === Number(control.dataset.skillId));
        await api("/api/student/skills", { method: "POST", body: JSON.stringify({ name: skill.name, category: skill.category, proficiency: Number(control.value) }) });
        notify("Proficiency updated."); navigate("skills");
      }
    } catch (error) { notify(error.message); }
  }

  function setAuthMode(register) {
    document.querySelector("#login-form").classList.toggle("hidden", register);
    document.querySelector("#register-form").classList.toggle("hidden", !register);
    document.querySelector("#auth-kicker").textContent = register ? "START WITH YOUR PROFILE" : "WELCOME BACK";
    document.querySelector("#auth-heading").textContent = register ? "Create your student account" : "Sign in to your arc";
    document.querySelector("#auth-description").textContent = register ? "A thoughtful profile makes the guidance more personal." : "Your profile and progress are right where you left them.";
    document.querySelector("#auth-switch-copy").textContent = register ? "Already have an account?" : "New to Career Arc?";
    document.querySelector("#auth-switch").textContent = register ? "Sign in" : "Create an account";
    document.querySelector("#login-message").textContent = "";
    document.querySelector("#register-message").textContent = "";
  }

  document.querySelector("#login-form").addEventListener("submit", onSubmit);
  document.querySelector("#register-form").addEventListener("submit", onSubmit);
  document.querySelector("#auth-switch").addEventListener("click", () => setAuthMode(document.querySelector("#register-form").classList.contains("hidden")));
  document.querySelector("#logout-button").addEventListener("click", async () => {
    try { await api("/api/auth/logout", { method: "POST", skipAuth: true }); } catch { /* The local refresh cookie may already have expired. */ }
    showAuth(); setAuthMode(false); notify("You have signed out.");
  });
  content.addEventListener("submit", onSubmit);
  content.addEventListener("click", onClick);
  content.addEventListener("change", onChange);
  navigation.addEventListener("click", onClick);

  (async () => {
    try {
      const user = await refreshAccessToken();
      if (user) showWorkspace(user);
      else showAuth();
    } catch { showAuth(); }
  })();
})();