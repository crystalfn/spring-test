package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRecordRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    public void buy(Trade trade, int id) {
        final Optional<TradeDto> optionalTradeDto = tradeRepository.findByRank(trade.getRank());
        if (optionalTradeDto.isPresent() && optionalTradeDto.get().getAmount() < trade.getAmount()) {
            final TradeDto tradeDto = optionalTradeDto.get();
            tradeDto.setAmount(trade.getAmount());
            tradeDto.setRsEventDto(rsEventRepository.findById(id).get());
            tradeRepository.save(tradeDto);
            tradeRecordRepository.save(tradeDto);
        }
        if (!optionalTradeDto.isPresent()) {
            final TradeDto tradeDto = TradeDto.builder()
                .rank(trade.getRank())
                .amount(trade.getAmount())
                .rsEventDto(rsEventRepository.findById(id).get())
                .build();
            tradeRepository.save(tradeDto);
            tradeRecordRepository.save(tradeDto);
        }
    }
}
