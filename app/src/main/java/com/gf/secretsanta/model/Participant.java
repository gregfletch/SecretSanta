package com.gf.secretsanta.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Participant implements Serializable {

    public Participant() {
        m_id = UUID.randomUUID().toString();
    }

    public String getId() {
        return m_id;
    }

    public void setId(String id) {
        m_id = id;
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public String getEmailAddress() {
        return m_emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        m_emailAddress = emailAddress;
    }

    public Participant getParticipantToBuyFor() {
        return m_buyFor;
    }

    public void setParticipantToBuyFor(Participant participant) {
        m_buyFor = participant;
    }

    public List<String> getExclusionList() {
        return m_excludeList;
    }

    public void setExclusionList(List<String> exclusionList) {
        m_excludeList = exclusionList;
    }

    public void addToExclusionList(String participantId) {
        if(m_excludeList == null) {
            m_excludeList = new ArrayList<>();
        }
        m_excludeList.add(participantId);
    }

    public void removeFromExclusionList(String participantId) {
        if(m_excludeList == null || m_excludeList.size() == 0) {
            return;
        }
        m_excludeList.remove(participantId);
    }

    private String m_id;
    private String m_name;
    private String m_emailAddress;
    private Participant m_buyFor;
    private List<String> m_excludeList;

    public static final String PARTICIPANT_INTENT_EXTRA_LABEL = "participant";
}
