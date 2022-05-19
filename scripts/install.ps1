param([Boolean]$build=$false)

Function CommandExists
{

 Param ($command)

 $oldPreference = $ErrorActionPreference

 $ErrorActionPreference = 'stop'

 try {if(Get-Command $command){RETURN $true}}

 Catch { RETURN $false}

 Finally {$ErrorActionPreference=$oldPreference}

} #end function test-CommandExists


if(CommandExists("python3")){
    Write-Output "Installing svshi ..."

    Write-Output "Setting SVSHI_HOME environment variable..."
    [System.Environment]::SetEnvironmentVariable('SVSHI_HOME', (get-item $PSScriptRoot).parent.FullName,[System.EnvironmentVariableTarget]::User)

    $Env:SVSHI_HOME = (get-item $PSScriptRoot).parent.FullName
    
    Write-Output "Setting Path environment variable..."
    [System.Environment]::SetEnvironmentVariable('Path', "$Env:Path;$Env:LOCALAPPDATA\svshi\bin",[System.EnvironmentVariableTarget]::User)
    
    if($build){
        if(-Not (CommandExists("sbt"))){
            Write-Error "sbt must be installed and recognized when running this script with -build true!"
            exit
        }
        Write-Output "Building SVSHI ..."
        Set-Location -Path $Env:SVSHI_HOME\src\core
        sbt pack
        Set-Location -Path $Env:SVSHI_HOME

    }

    Write-Output "Copying files ..."
    
    robocopy $Env:SVSHI_HOME\src\core\target\pack "$Env:LOCALAPPDATA\svshi" /E
    
    Write-Output "Installing Python dependencies ..."
    Set-Location -Path $Env:SVSHI_HOME\src
    python3 -m pip install -r requirements.txt
    
    Set-Location -Path $Env:SVSHI_HOME
    Write-Output "...svshi building done!"
} else {
    Write-Error "You must have python3 command available! Please run the install-python.ps1 script first."
}
pause



