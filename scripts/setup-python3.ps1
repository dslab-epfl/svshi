param([String]$currentDir=($MyInvocation.MyCommand.Path | split-path -parent | split-path -parent)) 


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

}


if (-Not ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] 'Administrator')) {
    if ([int](Get-CimInstance -Class Win32_OperatingSystem | Select-Object -ExpandProperty BuildNumber) -ge 6000) {
        $CommandLine = "-File `"" + $MyInvocation.MyCommand.Path + "`" " + $MyInvocation.UnboundArguments
        Start-Process -FilePath PowerShell.exe -Verb Runas -ArgumentList $CommandLine
        Exit
    }
    }


$python = 'python'
$py = 'py'
$python3 = 'python3'

if(CommandExists($python3)){
    Write-Output "python3 is already set up properly"
} else{
    if(CommandExists($python)){
        SetupPython($python)
    } else{
        if(CommandExists($py)){
            SetupPython($py)
        } else {
            Write-Error 'You must have a Python setup, accessible with `py`, `python` or `python3`!'
        }
    }
}

Set-Location $currentDir

Write-Warning "Please reboot your computer."
pause