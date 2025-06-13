package sf.sfis.miniesb.service;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import sf.sfis.miniesb.model.FidsAfttab;
import sf.sfis.miniesb.model.FidsGateHistory;
import sf.sfis.miniesb.repository.FidsAfttabRepository;
import sf.sfis.miniesb.repository.FidsGateHistoryRepository;

@Service
@RequiredArgsConstructor
public class FidsGateHistoryService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FidsGateHistoryService.class);
	private final FidsGateHistoryRepository fidsGateHistoryRepository;
	private final FidsAfttabRepository fidsAfttabRepository;
	
	@Transactional
	public FidsGateHistory saveFidsGateHistory(FidsGateHistory fidsGateHistory) {
		try {
			fidsGateHistory = fidsGateHistoryRepository.save(fidsGateHistory);
		} catch (Exception e) {
			LOGGER.error("saveFidsGateHistory: ", e);
		}
		return fidsGateHistory;
	}
	
	@Transactional
	public void deleteFidsGateHistory(FidsGateHistory fidsGateHistory) {
		try {
			fidsGateHistoryRepository.delete(fidsGateHistory);
		} catch (Exception e) {
			LOGGER.error("deleteFidsGateHistory: ", e);
		}
	}
	
	public boolean updateGateChangeHistory(FidsAfttab fidsAfttab) throws ClassNotFoundException, SQLException {
//		String DOMINT = "I";
//		String[] DOM_TTYP = {"05","06","50","51","56","58","59","60","61","62","63","64","65","66","68","76"};
//		if(Arrays.asList(DOM_TTYP).contains(fidsAfttab.getTtyp())) {
//			DOMINT="D";
//		}
//
//		String index = fidsAfttab.getHopo()+"_"+fidsAfttab.getAlc2()+"_"+DOMINT;
//    	HashMap<String,Integer> CounterOpenMap = new HashMap<String,Integer>();
//    	CounterOpenMap.put("DMK_FD_D", 120);
//    	CounterOpenMap.put("DMK_SL_D", 120);
//    	CounterOpenMap.put("DMK_DD_D", 120);
//    	CounterOpenMap.put("DMK_E3_D", 120);
//    	CounterOpenMap.put("DMK_SL_D", 120);
//    	CounterOpenMap.put("BKK_AF_D", 240);
//    	CounterOpenMap.put("BKK_AF_I", 240);
//    	CounterOpenMap.put("BKK_AY_D", 240);
//    	CounterOpenMap.put("BKK_AY_I", 240);
//    	CounterOpenMap.put("BKK_DY_D", 240);
//    	CounterOpenMap.put("BKK_DY_I", 240);
//    	CounterOpenMap.put("BKK_EY_D", 240);
//    	CounterOpenMap.put("BKK_EY_I", 240);
//    	CounterOpenMap.put("BKK_LH_D", 240);
//    	CounterOpenMap.put("BKK_LH_I", 240);
//    	CounterOpenMap.put("BKK_LX_D", 240);
//    	CounterOpenMap.put("BKK_LX_I", 240);
//    	CounterOpenMap.put("BKK_OS_D", 240);
//    	CounterOpenMap.put("BKK_OS_I", 240);
//    	CounterOpenMap.put("BKK_QR_D", 240);
//    	CounterOpenMap.put("BKK_QR_I", 240);
//    	CounterOpenMap.put("BKK_KL_D", 240);
//    	CounterOpenMap.put("BKK_KL_I", 240);
//    	CounterOpenMap.put("BKK_EK_D", 210);
//    	CounterOpenMap.put("BKK_EK_I", 210);
//    	CounterOpenMap.put("BKK_VZ_D", 120);
    	
    	int countopen = 180;//Default
//    	if(CounterOpenMap.get(index)!=null) {
//    		countopen=CounterOpenMap.get(index);
//    	}
    	
		try {
			Calendar current = Calendar.getInstance();
			Calendar counteropen_local = Calendar.getInstance();
			SimpleDateFormat sp = new SimpleDateFormat("yyyyMMddHHmmss");			
			counteropen_local.setTime(sp.parse(fidsAfttab.getSobt()));
			counteropen_local.add(Calendar.HOUR, 7);
			counteropen_local.add(Calendar.MINUTE, countopen*-1);
			if(current.before(counteropen_local)) {//Update comes before counter open
				System.out.println("Ignore Update Before Counter Open !!");
				return false;
			}
		}catch(Exception ex) {
			LOGGER.error("SOBT format error!!");
			return false;
		}
		
		boolean result = false;
		fidsAfttab.setGtd1(fidsAfttab.getGtd1()!=null&&fidsAfttab.getGtd1().trim().length()>0?fidsAfttab.getGtd1():"     ");
		fidsAfttab.setGtd2(fidsAfttab.getGtd2()!=null&&fidsAfttab.getGtd2().trim().length()>0?fidsAfttab.getGtd2():"     ");
		
		FidsGateHistory fidsGateHistory = new FidsGateHistory();
		fidsGateHistory.setUrno(fidsAfttab.getUrno().toString());
		if(fidsAfttab.getAtot()!=null && fidsAfttab.getAtot().trim().length()>0) {
			deleteFidsGateHistory(fidsGateHistory);
		}
		
		Optional<FidsGateHistory> queryFidsGateHistory = fidsGateHistoryRepository.findById(fidsAfttab.getUrno().toString());
		if (queryFidsGateHistory.isPresent()) {
			FidsGateHistory oldFidsGateHistory = queryFidsGateHistory.get();
		    if(oldFidsGateHistory.getNewgate1()!=null&&oldFidsGateHistory.getNewgate1().trim().equals(fidsAfttab.getGtd1().trim()) && oldFidsGateHistory.getNewgate2()!=null&&oldFidsGateHistory.getNewgate2().trim().equals(fidsAfttab.getGtd2().trim())) {
				//No Change
			}else if(oldFidsGateHistory.getNewgate1()!=null&&oldFidsGateHistory.getNewgate1().trim().length()<=0 && oldFidsGateHistory.getNewgate2()!=null&&oldFidsGateHistory.getNewgate2().trim().length()<=0) {
				//Just Add
			}else {
				LOGGER.info("Gate change detected!! URNO="+fidsAfttab.getUrno()+" New GTD1 "+fidsAfttab.getGtd1()+"("+oldFidsGateHistory.getNewgate1()+") , New GTD2 "+fidsAfttab.getGtd2()+"("+oldFidsGateHistory.getNewgate2()+")");
				fidsGateHistory.setUpdateTime(OffsetDateTime.now(ZoneOffset.UTC));
				fidsGateHistory.setNewgate1(fidsAfttab.getGtd1());
				fidsGateHistory.setNewgate2(fidsAfttab.getGtd2());
				fidsGateHistory.setOldgate1(oldFidsGateHistory.getNewgate1());
				fidsGateHistory.setOldgate2(oldFidsGateHistory.getNewgate2());
				fidsGateHistory.setFlno(fidsAfttab.getFlno());
				fidsGateHistory.setHopo(fidsAfttab.getHopo());
				fidsGateHistory.setSobt(fidsAfttab.getSobt());
				fidsGateHistory = saveFidsGateHistory(fidsGateHistory);
				result = fidsGateHistory!=null?true:false;
			}
		}
		return result;
	}

}
