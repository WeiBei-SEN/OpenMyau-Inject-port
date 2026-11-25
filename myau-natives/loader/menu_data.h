#pragma once
static std::mutex loaderMutex;
static std::wstring loaderStatus = L"Not loaded";
static bool busy = false, autoLog = false, cleanTemp = true;
static int processIndex = 0;
static std::vector<Candidate> processes;
static Combo* processCombo;
static Label* statusLabel;
static Button* loadButton;
static std::vector<Label*> logLabels;
static std::wstring logPath() {
    wchar_t path[MAX_PATH];
    if (!GetTempPathW(MAX_PATH, path)) return L"myau-native.log";
    return std::wstring(path) + L"myau-native.log";
}
static void openLog() { ShellExecuteW(nullptr, L"open", logPath().c_str(), nullptr, nullptr, SW_SHOWNORMAL); }
static void refreshProcesses() {
    processes.clear();
    for (const auto& c : findCandidates()) if (c.score > 0) processes.push_back(c);
    processIndex = 0;
    if (processCombo) {
        processCombo->opts.clear();
        for (const auto& c : processes) processCombo->opts.push_back(launcherOf(c.commandLine) + L" / PID " + std::to_wstring(c.pid));
        if (processes.empty()) processCombo->opts.push_back(L"No Minecraft process found");
    }
}
static bool resourceBytes(int id, const void*& bytes, size_t& size) {
    auto module = GetModuleHandleW(nullptr);
    auto r = FindResourceW(module, MAKEINTRESOURCEW(id), RT_RCDATA);
    if (!r) return false;
    auto h = LoadResource(module, r);
    bytes = LockResource(h); size = SizeofResource(module, r);
    return bytes && size;
}
static void loadClient() {
    std::lock_guard<std::mutex> guard(loaderMutex);
    if (busy) return;
    if (processes.empty() || processIndex < 0 || processIndex >= (int)processes.size()) {
        loaderStatus = L"未找到运行中的 Minecraft 1.8.9，请先启动到主菜单";
        return;
    }
    const void *dll, *jar; size_t dllSize, jarSize;
    if (!resourceBytes(101, jar, jarSize) || !resourceBytes(102, dll, dllSize)) {
        loaderStatus = L"Embedded DLL or Jar is missing"; return;
    }
    DWORD pid = processes[processIndex].pid;
    bool showLog = autoLog;
    busy = true; loaderStatus = L"Loading...";
    std::thread([=] {
        auto report = [](const std::wstring& line) {
            std::lock_guard<std::mutex> lock(loaderMutex);
            loaderStatus = line;
            FILE* out = _wfopen(logPath().c_str(), L"a, ccs=UTF-8");
            if (out) { fwprintf(out, L"[loader] %ls\n", line.c_str()); fclose(out); }
        };
        bool ok = runInjection(dll, dllSize, jar, jarSize, pid, report, report);
        { std::lock_guard<std::mutex> lock(loaderMutex); busy = false; if (ok) loaderStatus = L"Loaded — RSHIFT opens the menu; .help lists commands"; }
        if (showLog) openLog();
    }).detach();
}
static void updateLoader() {
    std::lock_guard<std::mutex> guard(loaderMutex);
    statusLabel->text = loaderStatus;
    loadButton->editable = !busy;
    static DWORD lastRead;
    if (GetTickCount() - lastRead < 1000) return;
    lastRead = GetTickCount();
    std::ifstream in(logPath().c_str());
    std::vector<std::string> lines; std::string line;
    while (std::getline(in, line)) { lines.push_back(line); if (lines.size() > logLabels.size()) lines.erase(lines.begin()); }
    for (size_t i = 0; i < logLabels.size(); ++i) {
        if (i >= lines.size()) { logLabels[i]->text.clear(); continue; }
        int len = MultiByteToWideChar(CP_UTF8, 0, lines[i].data(), (int)lines[i].size(), nullptr, 0);
        std::wstring wide(len, L' ');
        MultiByteToWideChar(CP_UTF8, 0, lines[i].data(), (int)lines[i].size(), wide.data(), len);
        logLabels[i]->text = wide;
    }
}
static Child* loaderGroup(int tab, const wchar_t* title) {
    auto c = new Child(title, 0, 24, 0, 24);
    menu.tabs[tab]->children.push_back(c); return c;
}
static void buildMenuData() {
    auto inject = loaderGroup(0, L"Inject");
    processCombo = inject->add(new Combo(L"Minecraft process", {}, &processIndex));
    inject->add(new Button(L"Refresh", refreshProcesses));
    loadButton = inject->add(new Button(L"Load", loadClient));
    statusLabel = inject->add(new Label(L"Not loaded"));
    inject->add(new Label(L"Start Minecraft 1.8.9 at its main menu."));
    auto client = loaderGroup(1, L"Client");
    client->add(new Label(L"OpenMyau 250910+10 / main 5ee5240"));
    client->add(new Label(L"Source revision: 5ee5240"));
    client->add(new Label(L"Minecraft: 1.8.9 / Windows x64"));
    client->add(new Label(L"Targets: Forge / Lunar / Badlion"));
    auto settings = loaderGroup(2, L"Settings");
    settings->add(new Checkbox(L"Open log after loading", &autoLog));
    settings->add(new Label(L"Payload files remain until the game exits."));
    auto scale = settings->add(new Combo(L"DPI scale", {L"100%", L"125%", L"150%", L"175%", L"200%"}, &dpiStep));
    scale->calls.push_back([](Element*) { applyDpi(dpiStep); });
    auto logs = loaderGroup(3, L"Log");
    logs->add(new Button(L"Open log", openLog));
    for (int i = 0; i < 20; ++i) logLabels.push_back(logs->add(new Label(L"")));
    refreshProcesses();
}
