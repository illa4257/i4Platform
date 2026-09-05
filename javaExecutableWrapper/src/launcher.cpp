#ifndef COMMAND
#error "Undefined COMMAND"
#endif

#ifndef ARGS
#error "Undefined ARGS"
#endif

#include <iostream>
#include <fstream>
#include <cstring>
#include <filesystem>
#include <windows.h>

static std::string trim(const std::string& str) {
    const size_t start = str.find_first_not_of(" \t");
    if (start == std::string::npos)
        return "";
    const size_t end = str.find_last_not_of(" \t");
    return str.substr(start, end - start + 1);
}

int main(int argc, char* argv[]) {
    PROCESS_INFORMATION pi;
    ZeroMemory(&pi, sizeof(pi));

    {
        char buffer[MAX_PATH];
        GetModuleFileNameA(nullptr, buffer, MAX_PATH);
        std::filesystem::path exe(buffer);
#ifdef CONFIG_FILENAME
        std::filesystem::path p = exe.parent_path() / (std::string(CONFIG_FILENAME) + ".ini");
#else
        std::filesystem::path p = exe.parent_path() / (exe.stem().string() + ".ini");
#endif

        std::cout << p << "\r\n";

        std::ifstream configFile(p);

        std::string command = COMMAND, startArgs = "", afterStartArgs = "";

        if (configFile.is_open()) {
            std::string line;
            while (std::getline(configFile, line)) {
                line = trim(line);
                if (line.empty() || line.front() == '#' || line.front() == ';')
                    continue;
                const size_t pos = line.find_first_of("=:");
                if (pos == std::string::npos || pos == 0)
                    continue;
                const std::string
                        key = trim(line.substr(0, pos)),
                        val = trim(line.substr(pos + 1));
                if (val.empty())
                    continue;
                if (key == "command")
                    command = val;
                else if (key == "start_args")
                    startArgs = ' ' + val;
                else if (key == "after_start_args")
                    startArgs = ' ' + val;
            }

            configFile.close();
        }

        std::string programArgs;
        for (int i = 1; i < argc; i++) {
            programArgs += ' ';
            if (const std::string_view arg = argv[i]; arg.find_first_of(" \t\r\n\"") != std::string::npos) {
                programArgs += '"';
                for (const char ch : arg)
                    if (ch == '"')
                        programArgs += "\\\"";
                    else
                        programArgs += ch;
                programArgs += '"';
            } else
                programArgs += arg;
        }

        STARTUPINFO si;
        ZeroMemory(&si, sizeof(si));
        si.cb = sizeof(si);

        command += startArgs;
        if constexpr (ARGS[0] != '\0') {
            command += ' ';
            command += ARGS;
        }
        command += afterStartArgs;
        command += programArgs;
        if (!CreateProcess(nullptr, command.data(), nullptr, nullptr, FALSE, 0, nullptr, nullptr, &si, &pi)) {
            const DWORD err = GetLastError();
            const std::string msg = "Failed to create a process (" + std::to_string(err) + ").";
            MessageBox(nullptr, msg.c_str(), "Error", MB_OK | MB_ICONERROR);
            std::cerr << "Failed to create a process (" << err << ")." << std::endl;
            return 1;
        }
    }

    WaitForSingleObject(pi.hProcess, INFINITE);

    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);

    return 0;
}