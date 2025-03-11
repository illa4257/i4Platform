#ifndef CONFIG_FILENAME
#error "Undefined CONFIG_FILENAME"
#endif

#ifndef ARGS
#error "Undefined ARGS"
#endif

#include <iostream>
#include <fstream>
#include <cstring>
#include <filesystem>
#include <windows.h>

std::string trim(const std::string& str) {
    size_t len = str.length(), start = 0, end = len;
    char ch;
    for (; start < len; start++) {
        ch = str[start];
        if (ch != ' ' && ch != '\t')
            break;
    }
    for (end--; end >= start; end--) {
        ch = str[end];
        if (ch != ' ' && ch != '\t')
            break;
    }
    return str.substr(start, end - start + 1);
}

size_t find(char* str, const char* chars) {
    size_t l = std::strlen(str);
    char ch;
    for (size_t i = 0; i < l; i++) {
        ch = str[i];
        for (const char* sch = chars; *sch != '\0'; sch++)
            if (ch == *sch)
                return i;
    }
    return std::string::npos;
}

size_t find(std::string& str, const char* chars) {
    size_t l = str.length();
    char ch;
    for (size_t i = 0; i < l; i++) {
        ch = str.at(i);
        for (const char* sch = chars; *sch != '\0'; sch++)
            if (ch == *sch)
                return i;
    }
    return std::string::npos;
}

int main(int argc, char* argv[]) {
    PROCESS_INFORMATION pi;
    ZeroMemory(&pi, sizeof(pi));

    {
        std::ifstream configFile(std::filesystem::path(argv[0]).parent_path() / (CONFIG_FILENAME + std::string(".ini")));

        std::string command = "java", startArgs = "";

        if (configFile.is_open()) {
            size_t pos;
            std::string line, key;
            while (std::getline(configFile, line)) {
                if (line.empty() || line.front() == '#' || line.front() == ';')
                    continue;
                pos = find(line, "=:");
                if (pos == std::string::npos || pos == 0)
                    continue;
                key = trim(line.substr(0, pos));
                if (key == "command") {
                    line = trim(line.substr(pos + 1));
                    if (!line.empty())
                        command = line;
                } else if (key == "java_start_args") {
                    line = trim(line.substr(pos + 1));
                    startArgs = line.empty() ? "" : ' ' + line;
                }
            }

            configFile.close();
        }

        std::string programArgs;
        for (int i = 1; i < argc; i++) {
            programArgs += ' ';
            if (find(argv[i], " \t\r\n\"") != std::string::npos) {
                programArgs += '"';
                for (char* ch = argv[i]; *ch != '\0'; ch++)
                    if (*ch == '"')
                        programArgs += "\\\"";
                    else
                        programArgs += ch;
                programArgs += '"';
            } else
                programArgs += argv[i];
        }

        STARTUPINFO si;
        ZeroMemory(&si, sizeof(si));
        si.cb = sizeof(si);

        if (!CreateProcess(NULL, (LPSTR) (command + startArgs + (ARGS[0] != '\0' ? ' ' + std::string(ARGS) : "") + programArgs).c_str(), NULL, NULL, FALSE, 0, NULL, NULL, &si, &pi)) {
            const DWORD err = GetLastError();
            MessageBox(NULL, ("Failed to create a process (" + std::to_string(err) + std::string(").")).c_str(), "Error", MB_OK | MB_ICONERROR);
            std::cerr << "Failed to create a process (" << err << ")." << std::endl;
            return 1;
        }
    }

    WaitForSingleObject(pi.hProcess, INFINITE);

    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);

    return 0;
}