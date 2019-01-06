package ${package};

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @Author: liushoulong
 * @Date: 2018/12/29 16:07
 */
public class DemoMain {

    public static Logger logger = LoggerFactory.getLogger(DemoMain.class);

    public static void main(String[] agrs) throws Exception{
        logger.info("启动流程 [ {} ]","start......");
        //创建流程引擎
        ProcessEngine engine = getProcessEngine();

        //流程文件装载--》db
        ProcessDefinition processDefinition = getProcessDefinition(engine);

        //运行流程文件,获取流程实例
        ProcessInstance processInstance = getProcessInstance(engine, processDefinition);

        Scanner scanner = new Scanner(System.in);
        //处理代办任务
        //当前流程实例存在并且不是结束节点
        while(processInstance != null && !processInstance.isEnded()) {

            //获取当前代办的任务task列表
            TaskService taskService = engine.getTaskService();
            List<Task> taskList = taskService.createTaskQuery().list();
            for(Task t:taskList){
                logger.info("待办任务 id = {} , name = {}",t.getId() ,t.getName());
                //对获取的任务，进行form表单处理
                FormService formService =  engine.getFormService();
                //获取当前task任务对应的表单对象
                TaskFormData formData = formService.getTaskFormData(t.getId());
                // 从当前表单中获取表单属性
                List<FormProperty> properties = formData.getFormProperties();
                Map<String,Object> varis = Maps.newHashMap();
                for(FormProperty p:properties){
                    String result = null;
                    logger.info("请输入 {} ", p.getName());
                    // 字符类型
                    if(StringFormType.class.isInstance(p.getType())){
                        result = scanner.nextLine();
                        varis.put(p.getId(), result);
                    }else if(DateFormType.class.isInstance(p.getType())){
                        result = scanner.nextLine();
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = format.parse(result);
                        varis.put(p.getId(), date);
                    }else{
                        logger.info("类型{}不匹配，输入结束", p.getType());
                    }
                    logger.info("你输入的内容是 {} ", result);
                }
                taskService.complete(t.getId(), varis);
                processInstance = engine.getRuntimeService().
                        createProcessInstanceQuery().
                        processInstanceId(processInstance.getId()).
                        singleResult();
            }
        }
        engine.close();
        logger.info("启动流程 [ {} ]","end......");
    }

    private static ProcessInstance getProcessInstance(ProcessEngine engine, ProcessDefinition processDefinition) {
        RuntimeService runtiomeService = engine.getRuntimeService();

        // 创建流程引擎实例,每一个用户操作都需要创建一个新的实例
        ProcessInstance processInstance = runtiomeService.startProcessInstanceById(processDefinition.getId());

        logger.info("当前操作的流程引擎 ProcessDefinitionKey = {} , DeploymentId = {}, ActivityId = {} ", processInstance.getProcessDefinitionKey(),processInstance.getDeploymentId(),processInstance.getActivityId());
        return processInstance;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine engine) {
        RepositoryService repositoryService = engine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        //部署到数据库,
        Deployment deployment = deploymentBuilder.deploy();
        //部署文件记录查询
        Deployment deploymentResult = repositoryService.createDeploymentQuery().deploymentId(deployment.getId()).singleResult();
        logger.info("部署文件查询 id = {} ", deploymentResult.getId());

        //流程文件查询
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        logger.info("流程文件 id = {} , name = {} , namespace = {} ", processDefinition.getId(),processDefinition.getName(),processDefinition.getCategory());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration config = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine engine = config.buildProcessEngine();

        //引擎名称  默认是 default
        String engineName = engine.getName();
        logger.info("引擎的默认名称 [{}] , 使用版本 [{}]", engineName,ProcessEngine.VERSION);
        return engine;
    }
}
