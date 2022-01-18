param([String]$currentDir=($MyInvocation.MyCommand.Path | split-path -parent)) 


Function CommandExists
{

 Param ($command)

 $oldPreference = $ErrorActionPreference

 $ErrorActionPreference = 'stop'

 try {if(Get-Command $command){RETURN $true}}

 Catch {RETURN $false}

 Finally {$ErrorActionPreference=$oldPreference}

}

Function SetupPython {

    Param ($pythonCommand)

    $commandPath = (get-command $pythonCommand).Path
    $commandDirectory = $commandPath | split-path -parent

    $link = "$commandDirectory\python3.exe"
    $target = "$commandDirectory\$pythonCommand.exe"
    Write-Output "creating symbolic link name=$link targetting $target"
    New-Item -ItemType SymbolicLink -Path "$link" -Target "$target"
    [System.Environment]::SetEnvironmentVariable('Path', "$Env:Path;$commandDirectory",[System.EnvironmentVariableTarget]::User)
    
    # For current session to install pip
    $Env:Path += ";$commandDirectory"

}

# Self-elevate the script if required
if (-Not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] 'Administrator')) {
    if ([int](Get-CimInstance -Class Win32_OperatingSystem | Select-Object -ExpandProperty BuildNumber) -ge 6000) {
     $CommandLine = "-File `"" + $MyInvocation.MyCommand.Path + "`" " + $MyInvocation.UnboundArguments
     Start-Process -FilePath PowerShell.exe -Verb Runas -ArgumentList $CommandLine
     Exit
    }
   }

if(CommandExists("python3")){
    Write-Output "python3 is already set up properly"
} else{
    if(CommandExists("python")){
        SetupPython("python")
    } else{
        if(CommandExists("py")){
            SetupPython("py")
        } else {
            Write-Error 'You must have a Python setup, accessible with `py`, `python` or `python3`!'
        }
    }
   
}
Write-Output "Installing pip ..."

Set-Location $currentDir\svshi

python3 'get-pip.py'

Write-Output ""
Write-Warning "Please add the directory asked by pip in the Warning message above to your Path environment variable."
Write-Warning "Please reboot your computer."
pause