using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Windows.Forms;

internal static class Jogar
{
    [STAThread]
    private static void Main()
    {
        try
        {
            string home = Path.GetDirectoryName(Application.ExecutablePath);
            string project = new[] { home, Path.Combine(home, "Quantum-Industries-git") }
                .FirstOrDefault(dir => File.Exists(Path.Combine(dir, "gradlew.bat")));
            if (project == null)
                throw new InvalidOperationException("Coloque Jogar.exe na raiz do repositorio Quantum Industries.");

            var start = new ProcessStartInfo
            {
                FileName = Path.Combine(Environment.SystemDirectory, "cmd.exe"),
                Arguments = "/d /c gradlew.bat runClient --console=plain",
                WorkingDirectory = project,
                UseShellExecute = false
            };
            string[] roots = { home, Path.GetDirectoryName(project) };
            foreach (string root in roots.Distinct())
            {
                string javaRoot = Path.Combine(root, @".local\java");
                if (Directory.Exists(javaRoot))
                {
                    string java = Directory.GetDirectories(javaRoot, "jdk-25*")
                        .OrderByDescending(dir => dir)
                        .FirstOrDefault(dir => File.Exists(Path.Combine(dir, @"bin\java.exe")));
                    if (java != null) start.EnvironmentVariables["JAVA_HOME"] = java;
                }

                string cache = Path.Combine(root, @".local\gradle");
                if (Directory.Exists(cache)) start.EnvironmentVariables["GRADLE_USER_HOME"] = cache;
            }

            using (Process game = Process.Start(start))
            {
                game.WaitForExit();
                if (game.ExitCode != 0)
                    throw new InvalidOperationException("O Minecraft nao iniciou ou encerrou com erro. Confira o Java 25 e o log em run\\logs\\latest.log.");
            }
        }
        catch (Exception error)
        {
            MessageBox.Show(error.Message, "Quantum Industries", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }
}
