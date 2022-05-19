
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
Function InstallPip {
    Param ($pythonCommand)

    $prm = 'get-pip.py'

    & $pythonCommand $prm

}

if(-Not (CommandExists("python3"))){
    Write-Error "python3 should be accessible!"
    exit
}

Write-Output "Installing pip ..."

Set-Location $currentDir\src

InstallPip("python3")

Set-Location $currentDir

pause