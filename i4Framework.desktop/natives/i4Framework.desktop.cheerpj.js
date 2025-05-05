async function Java_illa4257_i4Framework_desktop_cheerpj_CheerpJThemeDetector_run(
    lib, self
) {
    if (!window.matchMedia)
        return;
    const monitor = window.matchMedia('(prefers-color-scheme: dark)'),
         BaseTheme = await lib.illa4257.i4Framework.base.styling.BaseTheme;
    let watcher = null;
    monitor.addEventListener('change', e => {
        if (watcher != null)
            watcher(e.matches);
    });
    if (monitor.matches)
        await self.framework.onSystemThemeChange("dark", await BaseTheme.DARK);
    while (true)
        if (await new Promise(r => watcher = r))
            await self.framework.onSystemThemeChange("dark", await BaseTheme.DARK);
        else
            await self.framework.onSystemThemeChange("light", await BaseTheme.LIGHT);
}

export default {
    Java_illa4257_i4Framework_desktop_cheerpj_CheerpJThemeDetector_run
}