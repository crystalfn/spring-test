package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.TradeRecordDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRecordRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.ls.LSException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RsService {
    final RsEventRepository rsEventRepository;
    final UserRepository userRepository;
    final VoteRepository voteRepository;
    final TradeRepository tradeRepository;
    final TradeRecordRepository tradeRecordRepository;

    public RsService(RsEventRepository rsEventRepository,
                     UserRepository userRepository,
                     VoteRepository voteRepository,
                     TradeRepository tradeRepository,
                     TradeRecordRepository tradeRecordRepository) {
        this.rsEventRepository = rsEventRepository;
        this.userRepository = userRepository;
        this.voteRepository = voteRepository;
        this.tradeRepository = tradeRepository;
        this.tradeRecordRepository = tradeRecordRepository;
    }

    public List<RsEvent> getRsEventList() {
        List<RsEventDto> hasRankRsEvents = new ArrayList<>();
        List<RsEventDto> noRankRsEvents = new ArrayList<>();
        final List<RsEventDto> rsEventDtoList = rsEventRepository.findAll();
        rsEventDtoList.forEach(item -> {
            if (item.getRank() <= 0) {
                noRankRsEvents.add(item);
            } else {
                hasRankRsEvents.add(item);
            }
        });

        hasRankRsEvents.sort((o1, o2) -> o2.getVoteNum() - o1.getVoteNum());
        List<RsEventDto> sortRsEventList = new ArrayList<>();
        int index = 1;
        for (RsEventDto noRankRsEvent : noRankRsEvents) {
            for (RsEventDto hasRankRsEvent : hasRankRsEvents) {
                if (hasRankRsEvent.getRank() == index) {
                    sortRsEventList.add(hasRankRsEvent);
                    index++;
                }
            }
            sortRsEventList.add(noRankRsEvent);
            index++;
        }
        return sortRsEventList.stream()
            .map(item ->
                RsEvent.builder()
                    .eventName(item.getEventName())
                    .keyword(item.getKeyword())
                    .rank(item.getRank())
                    .userId(item.getId())
                    .voteNum(item.getVoteNum())
                    .build())
            .collect(Collectors.toList());
    }

    public void vote(Vote vote, int rsEventId) {
        Optional<RsEventDto> rsEventDto = rsEventRepository.findById(rsEventId);
        Optional<UserDto> userDto = userRepository.findById(vote.getUserId());
        if (!rsEventDto.isPresent()
            || !userDto.isPresent()
            || vote.getVoteNum() > userDto.get().getVoteNum()) {
            throw new RuntimeException();
        }
        VoteDto voteDto =
            VoteDto.builder()
                .localDateTime(vote.getTime())
                .num(vote.getVoteNum())
                .rsEvent(rsEventDto.get())
                .user(userDto.get())
                .build();
        voteRepository.save(voteDto);
        UserDto user = userDto.get();
        user.setVoteNum(user.getVoteNum() - vote.getVoteNum());
        userRepository.save(user);
        RsEventDto rsEvent = rsEventDto.get();
        rsEvent.setVoteNum(rsEvent.getVoteNum() + vote.getVoteNum());
        rsEventRepository.save(rsEvent);
    }

    public void buy(Trade trade, int id) throws Exception {
        final Optional<TradeDto> optionalTradeDto = tradeRepository.findByRank(trade.getRank());
        final Optional<RsEventDto> rsEventDto = rsEventRepository.findById(id);
        if (!rsEventDto.isPresent()) {
            throw new Exception("Invalid rsEventId");
        }

        if (optionalTradeDto.isPresent() &&
            optionalTradeDto.get().getAmount() > trade.getAmount()) {
            throw new Exception("Amount is not enough");
        } else {
            if (!optionalTradeDto.isPresent()) {
                final TradeDto tradeDto = TradeDto.builder()
                    .rank(trade.getRank())
                    .amount(trade.getAmount())
                    .rsEventId(id)
                    .build();
                tradeRepository.save(tradeDto);
            } else {
                final TradeDto tradeDto = optionalTradeDto.get();
                tradeDto.setAmount(trade.getAmount());
                tradeDto.setRsEventId(id);
                tradeRepository.save(tradeDto);
                rsEventRepository.deleteById(tradeDto.getRsEventId());
            }
            final TradeRecordDto tradeRecordDto = TradeRecordDto.builder()
                .rank(trade.getRank())
                .amount(trade.getAmount())
                .rsEventId(id)
                .build();
            tradeRecordRepository.save(tradeRecordDto);
        }
    }
}
