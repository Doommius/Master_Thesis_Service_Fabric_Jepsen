// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace ReliableCollectionsWebAPI.Controllers
{
    using System.Collections.Generic;
    using System.Threading;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Microsoft.ServiceFabric.Data;
    using Microsoft.ServiceFabric.Data.Collections;
    using System.Net.Http;
    using System;
    using System.Fabric;

    [Route("api/[controller]")]
    public class ReliableQueueController : Controller
    {
        private readonly IReliableStateManager StateManager;

        public ReliableQueueController(IReliableStateManager stateManager)
        {
            this.StateManager = stateManager;
        }

        // get api/ReliableQueue/ to get 
        [HttpGet("")]
        public async Task<IActionResult> Get()
        {
            IReliableQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<String>>("myReliableQueue");
           using (var txn = this.StateManager.CreateTransaction())
            {
                long returnvalue = await queue.GetCountAsync(txn);
                return this.Json((new KeyValuePair<string, long>("Queue Length", returnvalue)));
            }
        }

        // delete api/ReliableQueue/ to pop element
        [HttpDelete("")]
        public async Task<IActionResult> get()
        {
           
                        IReliableQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<String>>("myReliableQueue");
                        using (var txn = this.StateManager.CreateTransaction())
            {
                ConditionalValue<string> returnvalue  = await queue.TryDequeueAsync(txn);

                await txn.CommitAsync();

                if (returnvalue.HasValue)
                {
                    return this.Json(new KeyValuePair<string, string>("dequeue", returnvalue.Value));
                }
                else
                {
                    return new BadRequestResult();
                }
            }



        }

        // PUT /name/count
        [HttpPut("{value}")]
        public async Task<IActionResult> put(String value)
        {
            

            IReliableQueue<String> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<String>>("myReliableQueue");

            using (var txn = this.StateManager.CreateTransaction())
            {
                await queue.EnqueueAsync(txn, value);
                await txn.CommitAsync();
            
                
            }

           return new OkResult();



        }

    }
}