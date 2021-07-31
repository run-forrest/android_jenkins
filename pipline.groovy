pipeline {
    agent any
    options {
        disableConcurrentBuilds()
    }
    parameters {
      //选择git分支
        listGitBranches branchFilter: '.*', credentialsId: '', defaultValue: '', name: '', quickFilterEnabled: true, remoteURL: '', selectedValue: 'NONE', sortMode: 'ASCENDING_SMART', tagFilter: '.*', type: 'PT_BRANCH', description: ''
      // 设置参数 
      extendedChoice defaultValue: 'release', description: 'description', multiSelectDelimiter: ',', name: 'apk_type', quoteValue: false, saveJSONParameterToFile: false, type: 'PT_RADIO', value: 'debug,release'
        
    }
    stages {
        stage('参数处理') {
            steps {
                script {
                    String msg = "receive param: \n"
                    params.each {
                        msg = msg + "${it}\n"
                    }
                    println msg
                }
                script {
                    // 获取git分支处理
                  
                }
            }
        }
        
        stage('checkout') {
            steps {
                sh label: 'Resetting workspace', script: '''
                        git reset --hard
                        git clean -fd
                    '''
                dir('源码目录') {
                    sh label: 'Resetting workspace', script: '''
                        git reset --hard
                        git clean -fd
                        git fetch --prune
                    '''
                    sh label: 'Checkout  branch', script: "git checkout ${xxx}"
                    sh label: 'Pull  branch', script: "git pull origin ${xxx}"
                }
            }
        }
        
        stage('Android打包') {
            steps {
                script {
                    dir('源码目录') {
                        sh label: 'package', script: './gradlew clean'
                        if(params.apk_type == 'release') {
                            sh label: 'Package release', script: './gradlew assembleRelease'
                            archiveArtifacts: 'app/build/outputs/apk/release/*.apk'
                        } else {
                            sh label: 'Package debug', script: './gradlew assembleDebug'
                            archiveArtifacts: 'app/build/outputs/apk/debug/*.apk'
                        }
                    }
                }
            }
        }
        stage('发布到蒲公英平台'){
            steps {
                script {
                    dir('源码目录') {
                        String result = '';
                         if(params.apk_type == 'release') {
                             result= sh returnStdout: true ,script:
                               //自己填pgy的参数
                             '''
                             curl -F 'file=*' \
                              -F '_api_key=*' \
                              -F "installType=1" \
                              -F "uKey=*" \
                                https://www.pgyer.com/apiv2/app/upload  |jq '.data | .buildQRCodeURL'
                                '''
                         }
                         
                        println "图片的value ="+result
                        script {
                            env.pic = result 
                             
                        }
                    
                    }
                    }
                }
            }
        
        
        stage('飞书通知'){
            steps {
                script {
                     dir('source_code') {
                    println "图片的value =${env.pic}"
                    sh label: 'send', script: 
                        "python3 larkpush.py -V1=${env.pic}"
                     }
                }
            }
        }
    }

}




